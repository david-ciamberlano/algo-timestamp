package it.davidlab.algonot.service;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.IndexerClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.davidlab.algonot.domain.NotarizationCert;
import it.davidlab.algonot.domain.BlockchainData;
import it.davidlab.algonot.dto.VerificationData;
import it.davidlab.algonot.exception.ApiException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;


@Service
public class AlgorandService {

    private final Logger logger = LoggerFactory.getLogger(AlgorandService.class);

    @Value("${application.version}")
    private String APP_VERSION;

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

    public NotarizationCert notarize(String docName, String note, long docSize, byte[] docBytes) {

        logger.info("Try to notarize {} on the blockchain", docName);

        //sanitize the address
        String accountAddr = ACC_ADDRESS.trim();

        String docHash = DigestUtils.sha256Hex(docBytes);

        // build the packet code (to obtain a unique packet name)
        String packetName = accountAddr + docHash;
        String packetCode = DigestUtils.sha256Hex(packetName.getBytes(StandardCharsets.UTF_8));

        BlockchainData blockchainData = new BlockchainData(APP_VERSION, packetCode, docHash, note);

        // Build the json object
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
        String algoJsonRequest = gson.toJson(blockchainData);

        String txId;
        Long txRound;
        String txDate;
        try {

            // Notarize the document (write blockchainData on the blockchain)
            AlgodClient algoClient = new AlgodClient(ALGOD_API_ADDR, ALGOD_PORT, ALGOD_API_TOKEN);
            Address algoAddress = new Address(accountAddr);
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

            logger.info("Documenent notarized: {}", docName);

            txRound = algoClient.PendingTransactionInformation(txId).execute().body().confirmedRound;

            Map<String, Object> block = algoClient.GetBlock(txRound).execute().body().block;

            long timestamp = ((Integer) block.get("ts")).longValue();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            txDate = ZonedDateTime
                    .ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
                    .format(dateFormatter);

            logger.info("Document Notarized - date: {}", txDate);

            // create the json certificate with the registration data
            NotarizationCert notarizationCert =
                    new NotarizationCert(blockchainData, docName, docSize, accountAddr, txId, txRound, timestamp);

            return notarizationCert;

        } catch (Exception e) {
            logger.error("Algorand Notarization Exception", e);
            throw new RuntimeException(e.getMessage(), e);

        }
    }



    public VerificationData getDataFromTx (String txId) throws Exception {

        // get info from algorand transaction
        BlockchainData blockchainData;

        com.algorand.algosdk.v2.client.model.Transaction tx = indexerClient.searchForTransactions().txid(txId).execute().body().transactions.get(0);

        String noteObject = new String(tx.note);
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
        blockchainData = gson.fromJson(noteObject, BlockchainData.class);

        return new VerificationData(blockchainData.getNote(), blockchainData.getAppVersion(),
                blockchainData.getPacketCode(), blockchainData.getDocumentHash(),
                tx.sender, tx.confirmedRound, tx.roundTime);

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
        } else {
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
                } else {
                    return;
                }
            } else {
                throw new IllegalStateException("The transaction has been rejected");
            }
        }

        throw new IllegalStateException("Transaction not confirmed after " + timeout + " rounds!");
    }


}
