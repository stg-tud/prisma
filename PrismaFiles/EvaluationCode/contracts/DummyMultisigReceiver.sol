// SPDX-License-Identifier: UNLICENSED

pragma solidity >=0.7.0;
pragma experimental ABIEncoderV2;

contract DummyMultisigReceiver {
    
    
    function receiveNone() public {
        
    }
    
    function receiveOne(uint a) public {
        
    }
    
    function receiveTwo(uint a, uint b) public {
        
    }
    
    function receiveMore(bytes memory c) public {
        
    }
    
    function receiveCoins() public payable {
        
    }
    
    function receiveFailed() public {
        require(false);
    }
    
}
