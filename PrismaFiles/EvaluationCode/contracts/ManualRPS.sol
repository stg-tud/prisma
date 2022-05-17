// SPDX-License-Identifier: UNLICENSED
pragma solidity >=0.7.0;

// !!! LoC Counted !!!

contract ManualRPS {
    
    uint public result; //P1-Wins: 1, P2-Wins: 2, Draw: 3
    
    address[] parties;
    uint256 public deadline;
    bytes32 public commitment;
    uint8 public rps2;
    
    constructor(address[] memory _parties){
        
        parties = _parties;
        result = 6;
    }
    
    function commit(bytes32 _commitment) external {
        require(msg.sender == parties[0] && result == 6, "Already committed or wrong party to commit");
        
        commitment = _commitment;
        result = 5;
    }
    
    function move(uint8 _rps2) external {
        require(msg.sender == parties[1] && result == 5, "Already submitted or wrong party to submit move");
        rps2 = _rps2;
        deadline = block.timestamp + 10;
        result = 4;
    }
    
    function open(uint8 _rps1, uint nonce) external {
        require(msg.sender == parties[0] && result == 4, "No move submitted or wrong party to open");
        require(commitment == keccak256(abi.encode(_rps1, nonce)), "Wrong opening");
        
        if(rps2 % 3 == _rps1 % 3){
            result = 3;
        } else if (_rps1 % 3 == (rps2 + 1) % 3){
            result = 1;
        } else {
            result = 2;
        }
    }
    
    function timeout() external {
    	require(deadline <= block.timestamp, "Too early");
    	result = 2;
    }
    
    function getParties()  public view returns (address[] memory) {
    	return parties;
    }
}
