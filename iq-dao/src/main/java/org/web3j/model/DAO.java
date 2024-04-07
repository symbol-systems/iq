package org.web3j.model;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.11.1.
 */
@SuppressWarnings("rawtypes")
public class DAO extends Contract {
    public static final String BINARY = "608060405234801562000010575f80fd5b506040516200125538038062001255833981016040819052620000339162000184565b6001600160a01b038316620000815760405162461bcd60e51b815260206004820152600f60248201526e1d1c9d5cdd19594b9a5b9d985b1a59608a1b60448201526064015b60405180910390fd5b6001600160a01b038216620000c95760405162461bcd60e51b815260206004820152600d60248201526c1d1bdad95b8b9a5b9d985b1a59609a1b604482015260640162000078565b5f8160ff16118015620000e0575060648160ff1611155b6200011f5760405162461bcd60e51b815260206004820152600e60248201526d1c5d5bdc9d5b4b9a5b9d985b1a5960921b604482015260640162000078565b5f80546001600160a01b039485166001600160a01b03199091161790556001805460ff909216600160a01b026001600160a81b03199092169290931691909117179055620001d3565b80516001600160a01b03811681146200017f575f80fd5b919050565b5f805f6060848603121562000197575f80fd5b620001a28462000168565b9250620001b26020850162000168565b9150604084015160ff81168114620001c8575f80fd5b809150509250925092565b61107480620001e15f395ff3fe608060405234801561000f575f80fd5b5060043610610060575f3560e01c8063013cf08b146100645780635b8d495814610091578063aa98df39146100a6578063c9d27afe146100b9578063fc0c546a146100cc578063fdf97cb2146100f7575b5f80fd5b610077610072366004610b70565b610109565b604051610088959493929190610bfe565b60405180910390f35b6100a461009f366004610ce5565b61025b565b005b6100a46100b4366004610d29565b6105e3565b6100a46100c7366004610d63565b6107ff565b6001546100df906001600160a01b031681565b6040516001600160a01b039091168152602001610088565b5f546100df906001600160a01b031681565b60028181548110610118575f80fd5b905f5260205f2090600602015f91509050805f01805461013790610d95565b80601f016020809104026020016040519081016040528092919081815260200182805461016390610d95565b80156101ae5780601f10610185576101008083540402835291602001916101ae565b820191905f5260205f20905b81548152906001019060200180831161019157829003601f168201915b5050505050908060010180546101c390610d95565b80601f01602080910402602001604051908101604052809291908181526020018280546101ef90610d95565b801561023a5780601f106102115761010080835404028352916020019161023a565b820191905f5260205f20905b81548152906001019060200180831161021d57829003601f168201915b50505050600283015460038401546004909401549293909290915060ff1685565b5f546001600160a01b031633146102a75760405162461bcd60e51b815260206004820152600b60248201526a1b9bdd0b5d1c9d5cdd195960aa1b60448201526064015b60405180910390fd5b600254829081106102e45760405162461bcd60e51b81526020600482015260076024820152661a5b9d985b1a5960ca1b604482015260640161029e565b5f600282815481106102f8576102f8610dcd565b5f91825260209091206004600690920201015460ff16600281111561031f5761031f610bca565b146103545760405162461bcd60e51b8152602060048201526005602482015264195b99195960da1b604482015260640161029e565b6002818154811061036757610367610dcd565b5f91825260208083203384526005600690930201919091019052604090205460ff16156103c15760405162461bcd60e51b81526020600482015260086024820152671c995cdbdb1d995960c21b604482015260640161029e565b8151602e146104015760405162461bcd60e51b815260206004820152600c60248201526b7265736f6c76652e6970667360a01b604482015260640161029e565b600154604080516318160ddd60e01b815290515f926001600160a01b0316916318160ddd9160048083019260209291908290030181865afa158015610448573d5f803e3d5ffd5b505050506040513d601f19601f8201168201806040525081019061046c9190610de1565b90505f81116104ae5760405162461bcd60e51b815260206004820152600e60248201526d746f6b656e732e6d697373696e6760901b604482015260640161029e565b5f600285815481106104c2576104c2610dcd565b905f5260205f20906006020190505f82826003015483600201546104e69190610e0c565b6104f1906064610e25565b6104fb9190610e3c565b600154909150600160a01b900460ff1681101561054b5760405162461bcd60e51b815260206004820152600e60248201526d71756f72756d2e70656e64696e6760901b604482015260640161029e565b600182016105598682610ea9565b508160030154826002015411610570576002610573565b60015b60048301805460ff1916600183600281111561059157610591610bca565b021790555060048201546040517f72717eb8bb343495d5e94fd8d1d666f94d15a1397e0bb3ae34ea25adf2882976916105d391899160ff169086908a90610f65565b60405180910390a1505050505050565b5f546001600160a01b0316331461062a5760405162461bcd60e51b815260206004820152600b60248201526a1b9bdd0b5d1c9d5cdd195960aa1b604482015260640161029e565b6001546040516370a0823160e01b81523360048201525f916001600160a01b0316906370a0823190602401602060405180830381865afa158015610670573d5f803e3d5ffd5b505050506040513d601f19601f820116820180604052508101906106949190610de1565b116106cd5760405162461bcd60e51b81526020600482015260096024820152683737ba16bb37ba32b960b91b604482015260640161029e565b8051602e1461070e5760405162461bcd60e51b815260206004820152600d60248201526c70726f706f73616c2e6970667360981b604482015260640161029e565b600254612710116107565760405162461bcd60e51b81526020600482015260126024820152711c1c9bdc1bdcd85b0b995e1a185d5cdd195960721b604482015260640161029e565b600280546001810182555f919091526006027f405787fa12a823e0f2b7631cc41b3ba8828b3321ca811111fa75cd3aa3bb5ace01806107958382610ea9565b505f60028281018290556003830182905560048301805460ff19169055546107bf90600190611013565b90507f9c770c289ab5bf7e57cb1d23c8ceae993aea46eb64847072fd3d78ca60d3e43281846040516107f2929190611026565b60405180910390a1505050565b6002548290811061083c5760405162461bcd60e51b81526020600482015260076024820152661a5b9d985b1a5960ca1b604482015260640161029e565b5f6002828154811061085057610850610dcd565b5f91825260209091206004600690920201015460ff16600281111561087757610877610bca565b146108ac5760405162461bcd60e51b8152602060048201526005602482015264195b99195960da1b604482015260640161029e565b600281815481106108bf576108bf610dcd565b5f91825260208083203384526005600690930201919091019052604090205460ff16156109195760405162461bcd60e51b81526020600482015260086024820152671c995cdbdb1d995960c21b604482015260640161029e565b6001546040516370a0823160e01b81523360048201525f916001600160a01b0316906370a0823190602401602060405180830381865afa15801561095f573d5f803e3d5ffd5b505050506040513d601f19601f820116820180604052508101906109839190610de1565b116109be5760405162461bcd60e51b815260206004820152600b60248201526a1d9bdd194b99195b9a595960aa1b604482015260640161029e565b600283815481106109d1576109d1610dcd565b5f91825260208083203384526005600690930201919091019052604090205460ff16156109fc575f80fd5b6001546040516370a0823160e01b81523360048201525f916001600160a01b0316906370a0823190602401602060405180830381865afa158015610a42573d5f803e3d5ffd5b505050506040513d601f19601f82011682018060405250810190610a669190610de1565b9050600160028581548110610a7d57610a7d610dcd565b5f9182526020808320338452600692909202909101600501905260409020805460ff19169115159190911790558215610aec578060028581548110610ac457610ac4610dcd565b905f5260205f2090600602016002015f828254610ae19190610e0c565b90915550610b239050565b8060028581548110610b0057610b00610dcd565b905f5260205f2090600602016003015f828254610b1d9190610e0c565b90915550505b60408051858152336020820152841515818301526060810183905290517f7c2de587c00d75474a0c6c6fa96fd3b45dc974cd4e8a75f712bb84c950dce1b59181900360800190a150505050565b5f60208284031215610b80575f80fd5b5035919050565b5f81518084525f5b81811015610bab57602081850181015186830182015201610b8f565b505f602082860101526020601f19601f83011685010191505092915050565b634e487b7160e01b5f52602160045260245ffd5b60038110610bfa57634e487b7160e01b5f52602160045260245ffd5b9052565b60a081525f610c1060a0830188610b87565b8281036020840152610c228188610b87565b915050846040830152836060830152610c3e6080830184610bde565b9695505050505050565b634e487b7160e01b5f52604160045260245ffd5b5f82601f830112610c6b575f80fd5b813567ffffffffffffffff80821115610c8657610c86610c48565b604051601f8301601f19908116603f01168101908282118183101715610cae57610cae610c48565b81604052838152866020858801011115610cc6575f80fd5b836020870160208301375f602085830101528094505050505092915050565b5f8060408385031215610cf6575f80fd5b82359150602083013567ffffffffffffffff811115610d13575f80fd5b610d1f85828601610c5c565b9150509250929050565b5f60208284031215610d39575f80fd5b813567ffffffffffffffff811115610d4f575f80fd5b610d5b84828501610c5c565b949350505050565b5f8060408385031215610d74575f80fd5b8235915060208301358015158114610d8a575f80fd5b809150509250929050565b600181811c90821680610da957607f821691505b602082108103610dc757634e487b7160e01b5f52602260045260245ffd5b50919050565b634e487b7160e01b5f52603260045260245ffd5b5f60208284031215610df1575f80fd5b5051919050565b634e487b7160e01b5f52601160045260245ffd5b80820180821115610e1f57610e1f610df8565b92915050565b8082028115828204841417610e1f57610e1f610df8565b5f82610e5657634e487b7160e01b5f52601260045260245ffd5b500490565b601f821115610ea4575f81815260208120601f850160051c81016020861015610e815750805b601f850160051c820191505b81811015610ea057828155600101610e8d565b5050505b505050565b815167ffffffffffffffff811115610ec357610ec3610c48565b610ed781610ed18454610d95565b84610e5b565b602080601f831160018114610f0a575f8415610ef35750858301515b5f19600386901b1c1916600185901b178555610ea0565b5f85815260208120601f198616915b82811015610f3857888601518255948401946001909101908401610f19565b5085821015610f5557878501515f19600388901b60f8161c191681555b5050505050600190811b01905550565b8481525f6020610f7781840187610bde565b608060408401525f8554610f8a81610d95565b80608087015260a060018084165f8114610fab5760018114610fc557610ff0565b60ff1985168984015283151560051b890183019550610ff0565b8a5f52865f205f5b85811015610fe85781548b8201860152908301908801610fcd565b8a0184019650505b505050505083810360608501526110078186610b87565b98975050505050505050565b81810381811115610e1f57610e1f610df8565b828152604060208201525f610d5b6040830184610b8756fea26469706673582212207288dd8283b1385ef246e9556a934291b87dab80d76d22e108d32a1eeca47f3364736f6c63430008140033";

    private static String librariesLinkedBinary;

    public static final String FUNC_PROPOSALS = "proposals";

    public static final String FUNC_PROPOSE = "propose";

    public static final String FUNC_TALLY = "tally";

    public static final String FUNC_TOKEN = "token";

    public static final String FUNC_TRUSTEE = "trustee";

    public static final String FUNC_VOTE = "vote";

    public static final Event Proposal_EVENT = new Event("Proposal", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event Resolution_EVENT = new Event("Resolution", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint8>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event VOTED_EVENT = new Event("Voted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Address>() {}, new TypeReference<Bool>() {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected DAO(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected DAO(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected DAO(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected DAO(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<ProposalEventResponse> getProposalEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(Proposal_EVENT, transactionReceipt);
        ArrayList<ProposalEventResponse> responses = new ArrayList<ProposalEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ProposalEventResponse typedResponse = new ProposalEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.proposalId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.ipfsProposal = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ProposalEventResponse getProposalEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(Proposal_EVENT, log);
        ProposalEventResponse typedResponse = new ProposalEventResponse();
        typedResponse.log = log;
        typedResponse.proposalId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.ipfsProposal = (String) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<ProposalEventResponse> ProposalEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getProposalEventFromLog(log));
    }

    public Flowable<ProposalEventResponse> ProposalEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(Proposal_EVENT));
        return ProposalEventFlowable(filter);
    }

    public static List<ResolutionEventResponse> getResolutionEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(Resolution_EVENT, transactionReceipt);
        ArrayList<ResolutionEventResponse> responses = new ArrayList<ResolutionEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ResolutionEventResponse typedResponse = new ResolutionEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.proposalId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.ipfsProposal = (String) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.ipfsResolution = (String) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ResolutionEventResponse getResolutionEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(Resolution_EVENT, log);
        ResolutionEventResponse typedResponse = new ResolutionEventResponse();
        typedResponse.log = log;
        typedResponse.proposalId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.ipfsProposal = (String) eventValues.getNonIndexedValues().get(2).getValue();
        typedResponse.ipfsResolution = (String) eventValues.getNonIndexedValues().get(3).getValue();
        return typedResponse;
    }

    public Flowable<ResolutionEventResponse> ResolutionEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getResolutionEventFromLog(log));
    }

    public Flowable<ResolutionEventResponse> ResolutionEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(Resolution_EVENT));
        return ResolutionEventFlowable(filter);
    }

    public static List<VotedEventResponse> getVotedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(VOTED_EVENT, transactionReceipt);
        ArrayList<VotedEventResponse> responses = new ArrayList<VotedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            VotedEventResponse typedResponse = new VotedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.proposalId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.voter = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.support = (Boolean) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.tokensUsed = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static VotedEventResponse getVotedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(VOTED_EVENT, log);
        VotedEventResponse typedResponse = new VotedEventResponse();
        typedResponse.log = log;
        typedResponse.proposalId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.voter = (String) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.support = (Boolean) eventValues.getNonIndexedValues().get(2).getValue();
        typedResponse.tokensUsed = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        return typedResponse;
    }

    public Flowable<VotedEventResponse> votedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getVotedEventFromLog(log));
    }

    public Flowable<VotedEventResponse> votedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(VOTED_EVENT));
        return votedEventFlowable(filter);
    }

    public RemoteFunctionCall<Tuple5<String, String, BigInteger, BigInteger, BigInteger>> proposals(BigInteger param0) {
        final Function function = new Function(FUNC_PROPOSALS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint8>() {}));
        return new RemoteFunctionCall<Tuple5<String, String, BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple5<String, String, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple5<String, String, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple5<String, String, BigInteger, BigInteger, BigInteger>(
                                (String) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> propose(String _ipfsProposal) {
        final Function function = new Function(
                FUNC_PROPOSE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_ipfsProposal)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> tally(BigInteger _proposalId, String _ipfsResolution) {
        final Function function = new Function(
                FUNC_TALLY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_proposalId), 
                new org.web3j.abi.datatypes.Utf8String(_ipfsResolution)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> token() {
        final Function function = new Function(FUNC_TOKEN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> trustee() {
        final Function function = new Function(FUNC_TRUSTEE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> vote(BigInteger _proposalId, Boolean _support) {
        final Function function = new Function(
                FUNC_VOTE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_proposalId), 
                new org.web3j.abi.datatypes.Bool(_support)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static DAO load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new DAO(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static DAO load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new DAO(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static DAO load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new DAO(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static DAO load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new DAO(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<DAO> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String _trustee, String _token, BigInteger _quorum) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _trustee), 
                new org.web3j.abi.datatypes.Address(160, _token), 
                new org.web3j.abi.datatypes.generated.Uint8(_quorum)));
        return deployRemoteCall(DAO.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    public static RemoteCall<DAO> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String _trustee, String _token, BigInteger _quorum) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _trustee), 
                new org.web3j.abi.datatypes.Address(160, _token), 
                new org.web3j.abi.datatypes.generated.Uint8(_quorum)));
        return deployRemoteCall(DAO.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DAO> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String _trustee, String _token, BigInteger _quorum) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _trustee), 
                new org.web3j.abi.datatypes.Address(160, _token), 
                new org.web3j.abi.datatypes.generated.Uint8(_quorum)));
        return deployRemoteCall(DAO.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DAO> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String _trustee, String _token, BigInteger _quorum) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _trustee), 
                new org.web3j.abi.datatypes.Address(160, _token), 
                new org.web3j.abi.datatypes.generated.Uint8(_quorum)));
        return deployRemoteCall(DAO.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    public static void linkLibraries(List<Contract.LinkReference> references) {
        librariesLinkedBinary = linkBinaryWithReferences(BINARY, references);
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }

    public static class ProposalEventResponse extends BaseEventResponse {
        public BigInteger proposalId;

        public String ipfsProposal;
    }

    public static class ResolutionEventResponse extends BaseEventResponse {
        public BigInteger proposalId;

        public BigInteger status;

        public String ipfsProposal;

        public String ipfsResolution;
    }

    public static class VotedEventResponse extends BaseEventResponse {
        public BigInteger proposalId;

        public String voter;

        public Boolean support;

        public BigInteger tokensUsed;
    }
}
