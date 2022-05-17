# Case Studies

Bartoletti and Pompianu (FC'2017) identify five classes of smart contract applications: Financial, Notary, Game,  Wallet, and Library. Our case studies include at least one application per category. In addition, we consider scalability solutions.

## Financial

These apps include digital tokens, crowdfunding, escrowing, advertisement, insurances and sometimes Ponzi schemes. A study investigating all blocks mined until September 15th, 2018, found that 72.9% of the high-activity contracts are token contracts compliant to ERC-20 or ERC-721, which have an accumulated market capitalization of 12.7 billion USD. We have implemented a fungible Prisma token of the ERC-20 standard. Further, we implemented crowdfunding and escrowing case studies. These case studies demonstrate how to send and receive coins with Prisma, which is the basic functionality of financial applications. Other financial use cases can be implemented in Prisma with similar techniques.

## Notary
These contracts use the blockchain to store data immutably and persistently, e.g., to certify their ownership. We implemented a general-purpose notary contract enabling users to store arbitrary data, e.g., document hashes or images, together with a submission timestamp and the data owner. This case study demonstrates that Notaries are  expressible with Prisma.

## Games
We implemented TicTacToe, Rock-Paper-Scissors, Hangman and Chinese Checkers. Hangman evolves through multiple phases and hence benefits from the explicit control flow definition in Prisma more than the other game case studies. The game Chinese Checkers is more complex than the others, in regard to the number of parties, the game logic and the number of rounds, and hence, represents larger applications. Rock-Paper-Scissors illustrates how randomness for dApps is securely generated. Every Ethereum transaction, including the executions of contracts, is deterministic -- all participants can validate the generation of new blocks. Hence, secure randomness is negotiated among parties: in this case, by making use of timed commitments, i.e., all parties commit to a random seed share and open it after all commitments have been posted. The contract uses the sum of all seed shares as randomness. If one party aborts prior to opening its commitment, it is penalized. In Rock-Paper-Scissors both parties commit to their choice -- their random share -- and open it afterwards.
Other games of chance, e.g., gambling contracts, use the same technique.


## Wallet
A wallet contract manages digital assets, i.e., cryptocurrencies and tokens, and offers additional features such as shared ownership or daily transaction limits. At August 30, 2019, 3.9 M of 17.9 M (21%) deployed smart contracts have been different types of wallet contracts. Multi-signature wallets are a special type of wallet that provides a transaction voting mechanism by only executing transactions, which are signed by a fixed fraction of the set of owners. Wallets transfer money and call other contracts in their users stead depending on run-time input, demonstrating calls among contracts in Prisma. Further, a multi-signature wallet uses built-in features of the Ethereum VM for signature validation, i.e., data encoding, hash calculation, and signature verification, showing that these features are supported in Prisma.

## Libraries
As the cost of deploying a contract increases with the amount of code in Ethereum, developers try to avoid code repetitions. Contract inheritance does not help: child contracts simply copy the attributes and functions from the parent. Yet, one can outsource commonly used logic to library contracts that are deployed once and called by other contracts. For example, the TicTacToe dApp and the TicTacToe channel in our case studies share some logic, e.g., to check the win condition. To demonstrate libraries in Prisma, we include a TicTacToe library to our case studies and another on-chain executed TicTacToe dApp which uses such library instead of deploying the logic itself. Libraries use a call instruction similar to wallets, although the call target is typically known at deployment and can be hard-coded.

## Scalability solutions

State channels are scalability solutions, which enable a fixed group of parties to move their dApp to a non-blockchain consensus protocol: the execution falls-back to the blockchain in case of disputes. Similar to multi-signature wallets, state channels use built-in signature validation. We implemented a state channel for TicTacToe\footnote{A general solution is a much larger engineering effort and subject of industrial projects to demonstrate that Prisma supports state channels.


