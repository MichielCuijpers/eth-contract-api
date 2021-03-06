package org.adridadou.ethereum.smartcontract;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.adridadou.ethereum.blockchain.EthereumProxyEthereumJ;
import org.adridadou.ethereum.blockchain.Ethereumj;
import org.adridadou.ethereum.values.EthAccount;
import org.adridadou.ethereum.values.EthAddress;
import org.adridadou.ethereum.values.EthData;
import org.adridadou.ethereum.values.EthValue;
import org.adridadou.exception.EthereumApiException;
import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.core.CallTransaction;
import org.ethereum.core.CallTransaction.Contract;
import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionExecutor;

import static org.adridadou.ethereum.values.EthValue.wei;

/**
 * Created by davidroon on 20.04.16.
 * This code is released under Apache 2 license
 */
public class SmartContractEthereumJ implements SmartContract {
    public static final long GAS_LIMIT_FOR_CONSTANT_CALLS = 100_000_000_000_000L;
    private final EthAddress address;
    private final Contract contract;
    private final Ethereumj ethereum;
    private final EthereumProxyEthereumJ bcProxy;
    private final EthAccount sender;

    public SmartContractEthereumJ(Contract contract, Ethereumj ethereum, EthAccount sender, EthAddress address, EthereumProxyEthereumJ bcProxy) {
        this.contract = contract;
        this.ethereum = ethereum;
        this.sender = sender;
        this.bcProxy = bcProxy;
        this.address = address;
    }

    public List<CallTransaction.Function> getFunctions() {
        return Lists.newArrayList(contract.functions);
    }

    public Object[] callConstFunction(Block callBlock, String functionName, Object... args) {
        TransactionExecutor executor = executeLocally(callBlock, functionName, args);
        if(!executor.getReceipt().isSuccessful()) {
            throw new EthereumApiException(executor.getReceipt().getError());
        }
        return contract.getByName(functionName).decodeResult(executor.getResult().getHReturn());
    }

    private TransactionExecutor executeLocally(Block callBlock, final String functionName, final Object ... args) {
        Transaction tx = CallTransaction.createCallTransaction(0, 0, GAS_LIMIT_FOR_CONSTANT_CALLS,
                address.toString(), 0, contract.getByName(functionName), args);
        tx.sign(sender.key);

        Repository repository = getRepository().getSnapshotTo(callBlock.getStateRoot()).startTracking();

        try {
            TransactionExecutor executor = new TransactionExecutor
                    (tx, callBlock.getCoinbase(), repository, getBlockchain().getBlockStore(),
                            getBlockchain().getProgramInvokeFactory(), callBlock)
                    .setLocalCall(true);

            executor.init();
            executor.execute();
            executor.go();
            executor.finalization();

            return executor;
        } finally {
            repository.rollback();
        }
    }

    private BlockchainImpl getBlockchain() {
        return (BlockchainImpl) ethereum.getBlockchain();
    }

    private Repository getRepository() {
        return getBlockchain().getRepository();
    }


    public CompletableFuture<Object[]> callFunction(String functionName, Object... args) {
        return callFunction(wei(0), functionName, args);
    }

    @Override
    public CompletableFuture<Object[]> callFunction(String functionName, EthValue value, Object... arguments) {
        return callFunction(value, functionName, arguments);
    }

    public CompletableFuture<Object[]> callFunction(EthValue value, String functionName, Object... args) {
        return Optional.ofNullable(contract.getByName(functionName)).map((func) -> {
            EthData functionCallBytes = EthData.of(func.encode(args));
            return bcProxy.sendTx(value, functionCallBytes, sender, address)
                    .thenApply(receipt -> contract.getByName(functionName).decodeResult(receipt.getResult()));
        }).orElseThrow(() -> new EthereumApiException("function " + functionName + " cannot be found. available:" + getAvailableFunctions()));
    }

    private String getAvailableFunctions() {
        return Arrays.stream(contract.functions)
                .map(c -> c.name)
                .collect(Collectors.toList()).toString();
    }

    public Object[] callConstFunction(String functionName, Object... args) {
        return callConstFunction(getBlockchain().getBestBlock(), functionName, args);
    }

    public EthAddress getAddress() {
        return address;
    }
}
