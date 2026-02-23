// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

interface I_DAO {
enum ProposalStatus { Pending, Approved, Rejected }
enum State { Inactive, Active, Retired }

struct Proposal {
string ipfsProposal;
string ipfsResolution;
uint256 votesFor;
uint256 votesAgainst;
ProposalStatus status;
mapping(address => bool) hasVoted;
}

event Proposal(uint256 proposalId, string ipfsProposal);
event Vote(uint256 proposalId, address voter, bool support, uint256 tokensUsed);
event Resolution(uint256 proposalId, ProposalStatus status, string ipfsProposal, string ipfsResolution);
event Activity(uint256 proposalId, string ipfsAction);
event Observation(string _ipfsObservation);

function state(State calldata status) external;

function propose(string calldata _ipfsProposal) external;
function vote(uint256 _proposalId, bool _support) external;
function tally(uint256 _proposalId, string calldata _ipfsResolution) external;
function execute(uint256 _proposalId, string calldata _ipfsAction) external;
function appoint(address _newTrustee) external;
}
