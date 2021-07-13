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
import it.davidlab.algonot.dom.AlgoNotarizationCert;
import it.davidlab.algonot.dom.AlgoNotarizationReq;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class AlgorandService {

    Logger logger = LoggerFactory.getLogger(AlgorandService.class);

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


    public void notarize(MultipartFile docFile) {

        String docName = docFile.getOriginalFilename();
        logger.info("Try notarization for: {}", docName);

        long docSize = docFile.getSize();
        String messageDigest;
        try {
            messageDigest = DigestUtils.sha256Hex (docFile.getBytes());
        }
        catch (IOException ex) {
            logger.error("File Exception", ex);
            throw new RuntimeException("Can't read file Exception", ex);
        }

        AlgoNotarizationReq notarizationRequest = new AlgoNotarizationReq(messageDigest, docName, docSize, new Date());

        // Build the json object
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
        String algoJsonRequest = gson.toJson(notarizationRequest);

        String txId;
        Long txRound;
        String txDate;
        try {
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
            AlgoNotarizationCert notarizationCert = new AlgoNotarizationCert(notarizationRequest, ACC_ADDRESS, txId);
            String algoJsonCert = gson.toJson(notarizationCert);

            createPacket(txRound, docName, docFile.getBytes(), algoJsonCert);

        } catch (Exception ex) {
            logger.error("Algorand Client creation Exception", ex);
            throw new RuntimeException("Client Exception", ex);
        }
    }

    private void createPacket (long blockNum, String docName, byte[] docBytes, String certificate ) {

        String zipName = "algonot-" + blockNum + ".zip";
        String certName = "certificate-" + blockNum + ".json";
        try (FileOutputStream fos = new FileOutputStream(zipName);
                ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            ZipEntry zipEntryDoc = new ZipEntry(docName);
            zipOut.putNextEntry(zipEntryDoc);

            zipOut.write(docBytes, 0, docBytes.length);

            ZipEntry zipEntryCert = new ZipEntry(certName);
            zipOut.putNextEntry(zipEntryCert);

            byte[] certBytes = Strings.toByteArray(certificate);
            zipOut.write(certBytes, 0, certBytes.length);

        }
        catch (IOException ex) {
            logger.error("Algorand ZIP creation Exception", ex);
        }

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
