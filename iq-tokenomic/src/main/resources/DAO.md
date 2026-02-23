
The heart of a DAO is a smart contract - a digital rulebook, if you like.

The governance of a DAO is observed as events on the blockchain.

This ensures the auditability, explain-ability and transparency of its governance.

Its knowledge can be encrypted by the trustee or observable, as required.

It curates its own unique knowledge from trustees, observations, logic and inference.

1. **Proposals:** Trustees publish proposals as linked data on IPFS then notifies the DAO.

2. **Voting:** Token holders vote on proposals, with the DAO verifying and recording votes.

3. **Resolution:** If quorum is reached, the DAO tallies votes, updates proposal status and notifies stakeholders.

4. **Observations:** The Trustees can share facts, insights and observations with the DAO on IPFS .

## Governance flow

Here we see the interaction between the Token Holders, the DAO smart contract, the Trustee, and the Interplanetary Filesystem (IPFS):


```mermaid
sequenceDiagram
participant Token
participant DAO
participant Trustee
participant IPFS

Trustee->>IPFS: Store proposal details
IPFS-->>Trustee: IPFS proposal hash
Trustee->>DAO: propose(ipfsProposal)
DAO->>DAO: Create new proposal
DAO-->>Trustee: Proposal(proposalId, ipfsProposal)

Token->>DAO: vote(proposalId, support)
DAO->>Token: Check trustee's token balance
Token-->>DAO: Token balance

DAO->>DAO: Register vote
DAO-->>Trustee: Voted(proposalId, trustee, support, tokensUsed)

alt Quorum reached
DAO->>DAO: Tally votes
DAO->>DAO: Update proposal status
DAO-->>Trustee: Resolution(proposalId, status, ipfsProposal, ipfsResolution)
else Quorum not reached
DAO-->>Trustee: Proposal still pending
end

Trustee->>IPFS: Store resolution receipt
IPFS-->>Trustee: IPFS resolution hash
Trustee->>DAO: tally(proposalId, ipfsResolution)

Trustee->>IPFS: Store execution activity
IPFS-->>Trustee: IPFS activity hash
Trustee->>DAO: execute(proposalId, ipfsAction)
DAO-->>Trustee: Activity(proposalId, ipfsAction)

Trustee->>DAO: appoint(newTrustee)
DAO-->>Trustee: New trustee appointed

```
