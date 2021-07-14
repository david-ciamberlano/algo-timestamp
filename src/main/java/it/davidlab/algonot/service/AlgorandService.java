package it.davidlab.algonot.service;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.IndexerClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.NodeStatusResponse;
import com.algorand.algosdk.v2.client.model.PendingTransactionResponse;
import com.algorand.algosdk.v2.client.model.TransactionParametersResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.davidlab.algonot.dom.NotarizationCert;
import it.davidlab.algonot.dom.BlockchainData;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


@Service
public class AlgorandService {

    private final Logger logger = LoggerFactory.getLogger(AlgorandService.class);

    @Value("${algorand.api.address}")
    private String ALGOD_API_ADDR;
    @Value("${algorand.api.port}")
    private Integer ALGOD_PORT;
    @Value("${algorand.api.token}")
    private String ALGOD_API_TOKEN;

    @Value("${algorand.api.indexer.address}")
    private String INDEXER_API_ADDR;
    @Value("${algorand.api.indexer.port}")
    private Integer INDEXER_API_PORT;


    @Value("${algorand.account.passfrase}")
    private String ACC_PASSFRASE;
    @Value("${algorand.account.address}")
    private String ACC_ADDRESS;

    private AlgodClient client;
    private IndexerClient indexerClient;

    @PostConstruct
    public void init() {
        client = new AlgodClient(ALGOD_API_ADDR, ALGOD_PORT, ALGOD_API_TOKEN);
        indexerClient = new IndexerClient(INDEXER_API_ADDR, INDEXER_API_PORT);
    }


    public NotarizationCert notarize(String docName, long docSize, byte[] docBytes) {

        logger.info("Try notarization for: {}", docName);

        String packetName = DigestUtils.sha256Hex(docName.getBytes(StandardCharsets.UTF_8));
        String documentHash = DigestUtils.sha256Hex(docBytes);

        BlockchainData blockchainData = new BlockchainData(documentHash, packetName);

        // Build the json object
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
        String algoJsonRequest = gson.toJson(blockchainData);

        String txId;
        Long txRound;
        String txDate;
        try {

            // Notarize the document (write blockchainData on the blockchain)
            AlgodClient algoClient = new AlgodClient(ALGOD_API_ADDR, ALGOD_PORT, ALGOD_API_TOKEN);
            Address algoAddress = new Address(ACC_ADDRESS);
            Account algoAccount = new Account(ACC_PASSFRASE);
            TransactionParametersResponse params = algoClient.TransactionParams().execute().body();
            Transaction txn = Transaction.PaymentTransactionBuilder()
                    .sender(algoAddress)
                    .note(algoJsonRequest.getBytes())
                    .amount(0)
                    .receiver(algoAddress)
                    .suggestedParams(params)
                    .build();

            SignedTransaction signedTxn = algoAccount.signTransaction(txn);
            byte[] encodedTxBytes = Encoder.encodeToMsgPack(signedTxn);

            txId = algoClient.RawTransaction().rawtxn(encodedTxBytes).execute().body().txId;
            waitForConfirmation(txId, 5);

            txRound = algoClient.PendingTransactionInformation(txId).execute().body().confirmedRound;

            Map<String, Object> block = algoClient.GetBlock(txRound).execute().body().block;

            Integer timestamp = (Integer)block.get("ts");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            txDate = ZonedDateTime
                    .ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
                    .format(dateFormatter);

            logger.info("Notarized: {} - date: {}", docName, txDate);

            // create the json certificate with the registration data
            NotarizationCert notarizationCert =
                    new NotarizationCert(blockchainData, docName, docSize, ACC_ADDRESS, txId, txRound);
            return notarizationCert;

        } catch (Exception ex) {
            logger.error("Algorand Client creation Exception", ex);
            throw new RuntimeException("Client Exception", ex);
        }
    }


    public boolean verify(byte[] packet) throws IOException {

        byte[] certificateContent = new byte[0];
        byte[] documentContent = new byte[0];

        //extract info from the zip
        try (ByteArrayInputStream zipIS = new ByteArrayInputStream(packet);
             ZipInputStream zis = new ZipInputStream(zipIS)) {

            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                if (!zipEntry.isDirectory()) {
                    if ("certificate.json".equals(zipEntry.getName())) {
                        certificateContent = zis.readAllBytes();
                    } else {
                        documentContent = zis.readAllBytes();
                    }
                }

                zipEntry = zis.getNextEntry();
            }
        }

        if (certificateContent.length == 0 || documentContent.length == 0) {
            throw new IOException("Wrong packet");
        }

        Reader certReader = new InputStreamReader(new ByteArrayInputStream(certificateContent),StandardCharsets.UTF_8);
        NotarizationCert certificate = new Gson().fromJson(certReader, NotarizationCert.class);

        if ( certificate.checkNull()) {
            throw new IOException("Wrong certificate");
        }

        logger.info("Same Size: " + (certificate.getDocSize() == documentContent.length));

        String documentHash = DigestUtils.sha256Hex(documentContent);
        logger.info("Same Hash: " + (certificate.getBlockchainData().getDocumentHash().equals(documentHash)) );

        logger.info(certificate.getOriginalFileName());

        return true;
    }


    /**
     * Wait for transaction confirmation
     *
     * @param txId
     * @param timeout
     * @throws Exception
     */
    public void waitForConfirmation(String txId, int timeout) throws Exception {

        Long txConfirmedRound = -1L;
        Response<NodeStatusResponse> statusResponse = client.GetStatus().execute();

        long lastRound;
        if (statusResponse.isSuccessful()) {
            lastRound = statusResponse.body().lastRound + 1L;
        }
        else {
            throw new IllegalStateException("Cannot get node status");
        }

        long maxRound = lastRound + timeout;

        for (long currentRound = lastRound; currentRound < maxRound; currentRound++) {
            Response<PendingTransactionResponse> response = client.PendingTransactionInformation(txId).execute();

            if (response.isSuccessful()) {
                txConfirmedRound = response.body().confirmedRound;
                if (txConfirmedRound == null) {
                    if (!client.WaitForBlock(currentRound).execute().isSuccessful()) {
                        throw new Exception();
                    }
                }
                else {
                    return;
                }
            } else {
                throw new IllegalStateException("The transaction has been rejected");
            }
        }

        throw new IllegalStateException("Transaction not confirmed after " + timeout + " rounds!");
    }


}
