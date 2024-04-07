// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./I_DAO.sol";
import "./I_ERC20.sol";

/**
 * @title A contract for distributed governance
 * @dev Token holders vote on matters that are proposed by the trustee
 */
contract DAO is I_DAO {
    address public trustee; // Address of the trustee
    address public token; // Address of the ERC20 token used for voting
    uint8 quorum; // 1-100 percentage of voted tokens needed for quorum
    Proposal[] public proposals; // Array to store all proposals
    State public state ; // Current state { Inactive, Active, Retired }

    modifier onlyTrustee() {
        require(msg.sender == trustee, "not-trusted"); // Ensure only the trustee can execute
        _;
    }

    modifier onlyVoter() {
        require(IERC20(token).balanceOf(msg.sender) > 0, "vote.invalid"); // A valid voter has a non-zero balance of tokens
        _;
    }

    modifier onlyValidProposal(uint256 _proposalId) {
        require(_proposalId < proposals.length, "invalid"); // Ensure the proposal id is valid
        require(proposals[_proposalId].status == ProposalStatus.Pending, "ended"); // Ensure the proposal is still pending
        require(!proposals[_proposalId].hasVoted[msg.sender], "resolved"); // Ensure the sender has not already voted on the proposal
        _;
    }

    modifier onlyIPFS(string _ipfsHash) {
        require(bytes(_ipfsHash).length == 46, "ipfs.invalid");
    }

    modifier onlyActive() {
        require(state != State.Retired, "retired");
        require(state != State.Inactive, "inactive");
    }

    /**
     * @dev Constructor function to initialize the DAO contract.
     * @param _trustee Address of the trustee who has administrative rights.
     * @param _token Address of the ERC20 token contract used for voting.
     * @param _quorum Percentage of voted tokens needed for quorum (1-100).
     */
    constructor(address _trustee, address _token, uint8 _quorum) {
        require(_trustee != address(0), "trustee.invalid"); // Ensure trustee address is not null
        require(_token != address(0), "token.invalid"); // Ensure token address is not null
        require(_quorum > 0 && _quorum <= 100, "quorum.invalid"); // Ensure quorum is within valid range (1-100)
        trustee = _trustee; // Set the trustee address
        token = _token; // Set the address of the ERC20 token
        quorum = _quorum; // Set the quorum (percentage) of votes required to resolve
        state = State.Active; // The DAO is default active
    }

    /** @dev Change the state of the DAO
            An _ipfsReason is an JSON-LD document conforming to (at least) the DAO and PROVO RDFS schemas.
        * @param _state The operational state of the DAO.
        * @param _ipfsReason IPFS hash of the proposal details.
    */
    function state(State calldata _state, string memory _ipfsReason) external onlyTrustee onlyIPFS(_ipfsReason) {
        require(!state.Retired, "retired"); // Retired DAO can't change state
        state = _state;
        observe(_ipfsReason);
    }

    /**
     * @dev Creates a new proposal documented on IPFS then emits a Proposal event.
            An _ipfsProposal is an JSON-LD document conforming to (at least) the DAO and PROVO RDFS schemas.
     * @param _ipfsProposal IPFS hash of the proposal details.
     */
    function propose(string memory _ipfsProposal) external override onlyTrustee onlyIPFS(_ipfsProposal) onlyActive {
        Proposal storage newProposal = proposals.push();
        newProposal.ipfsProposal = _ipfsProposal;
        newProposal.votesFor = 0;
        newProposal.votesAgainst = 0;
        newProposal.status = ProposalStatus.Pending;

        uint256 proposalId = proposals.length - 1;
        emit Proposal(proposalId, _ipfsProposal);
    }

    /**
     * @dev Allows a voter to cast a vote on a proposal then emits a Voted event.
     * @param _proposalId ID of the proposal.
     * @param _support Boolean indicating whether the voter supports or opposes the proposal.
     */
    function vote(uint256 _proposalId, bool _support) external override onlyValidProposal(_proposalId) onlyVoter onlyActive {
        require(false == proposals[_proposalId].hasVoted[msg.sender]); // Ensure the sender has not already voted on the proposal

        uint256 tokensUsed = IERC20(token).balanceOf(msg.sender); // Get the number of tokens used for voting

        proposals[_proposalId].hasVoted[msg.sender] = true; // Mark the sender as voted

        if (_support) {
            proposals[_proposalId].votesFor += tokensUsed; // Increment votes in favor
        } else {
            proposals[_proposalId].votesAgainst += tokensUsed; // Increment votes against
        }

        emit Vote(_proposalId, msg.sender, _support, tokensUsed);
    }

    /**
     * @dev Tallies the votes for a proposal, updates the status and emits a Resolution event.
            A _ipfsResolution receipt is an JSON-LD document conforming to (at least) the DAO and PROVO RDFS schemas.
     * @param _proposalId ID of the proposal.
     * @param _ipfsResolution IPFS hash of the fact graph.
     */
    function tally(uint256 _proposalId, string memory _ipfsResolution) external override onlyTrustee onlyValidProposal(_proposalId) onlyIPFS(_ipfsResolution) onlyActive {
        uint256 totalTokens = IERC20(token).totalSupply();
        require(totalTokens > 0, "tokens.missing"); // Ensure a non-zero supply of tokens

        Proposal storage proposal = proposals[_proposalId];

        uint256 quorumCheck = (proposal.votesFor + proposal.votesAgainst) * 100 / totalTokens;
        require(quorumCheck >= quorum, "quorum.pending"); // Check if the quorum is met

        proposal.ipfsResolution = _ipfsResolution;
        proposal.status = (proposal.votesFor > proposal.votesAgainst) ? ProposalStatus.Approved : ProposalStatus.Rejected; // Set proposal status based on votes

        emit Resolution(_proposalId, proposal.status, proposal.ipfsProposal, _ipfsResolution);
    }

    /**
     * @dev Request action on behalf of an approved proposal, and emits a ActionRequest event.
            A _ipfsActionRequest is an JSON-LD document conforming to (at least) the DAO and PROVO RDFS schemas.
     * @param _proposalId ID of the proposal.
     * @param  _ipfsActionRequest IPFS hash of the fact graph claim.
     */
    function execute(uint256 _proposalId, string memory _ipfsActionRequest) external override onlyTrustee onlyValidProposal(_proposalId) onlyIPFS(_ipfsActionRequest) onlyActive {
        Proposal storage proposal = proposals[_proposalId];
        require(proposal.status == ProposalStatus.Approved, 'not.approved');
        emit ActionRequest(_proposalId, _ipfsActionRequest);
    }

    /**
     * @dev An insight or observation.
            A _ipfsObservation is an JSON-LD document conforming to (at least) the DAO and PROVO RDFS schemas.
     * @param _proposalId ID of the proposal.
     * @param  _ipfsObservation IPFS hash of the fact graph claim.
     */
    function observe(string memory _ipfsObservation) external override onlyTrustee onlyIPFS(_ipfsObservation) onlyActive {
        emit Observation(_ipfsObservation);
    }

    /**
     * @dev Function to transfer ownership of the DAO contract to a new address.
     * @param _newTrustee The address of the new trustee.
     * @param  _ipfsAppointment IPFS hash of the fact graph claim.
     */
    function appoint(address _newTrustee, string memory _ipfsAppointment) external onlyTrustee onlyIPFS(_ipfsAppointment) {
        require(_newTrustee != address(0), "trustee.invalid");
        trustee = _newTrustee;
        observe(_ipfsAppointment);
    }
}
