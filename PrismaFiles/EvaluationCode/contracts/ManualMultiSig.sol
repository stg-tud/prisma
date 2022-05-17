// SPDX-License-Identifier: UNLICENSED
pragma solidity >=0.7.0;
pragma experimental ABIEncoderV2;

contract ManualMultiSig {
    
    struct Signature {
        uint8 v;
        bytes32 r;
        bytes32 s;
    }
    
    uint public nonce;
    uint public threshold;
    address[] parties;
    
    constructor(uint _threshold, address[] memory _parties) {
        nonce = 0;
        threshold = _threshold;
        parties = _parties;
    }
    
    function execute(Signature[] memory signatures, uint8[] memory indices, uint timeout,
    	address destination, uint value, bytes memory data) public {
        
        //Checks: corret array length and before submission timeout
        require(signatures.length == threshold, "Wrong number of signatures");
        require(indices.length == threshold, "Wrong number of indices");
        require(block.timestamp < timeout, "Too late to submit");
        require(address(this).balance >= value, "Not enough coins");
        
        //Calc Hash
        bytes32 _prefixedHash = keccak256(abi.encodePacked("\x19Ethereum Signed Message:\n32",
        	keccak256(abi.encodePacked(address(this), nonce, timeout, destination, value, data))));

        //Check signatures
        address last = address(0);
        for(uint i = 0; i < threshold; i++){
            address rec = ecrecover(_prefixedHash, signatures[i].v, signatures[i].r, signatures[i].s);
            //require(rec > last && rec == parties[indices[i]], "Wrong signature, bad ordering or duplicate signature");                      
            require(rec > last, "Bad ordering");
            require(rec == parties[indices[i]], "Wrong signature");
            last = rec;
        }
        
        nonce = nonce + 1;
        (bool success,) = destination.call{value:value}(data);
        require(success, "Call failed");
        
    }
    
    function getParties() public view returns (address[] memory) {
    return(parties);
  } 
    
    receive() external payable {
        //Just to receive coins
    }
}
