// SPDX-License-Identifier: UNLICENSED
pragma solidity >=0.7.0;

contract ManualEscrow {
    
    uint public phase = 2;
    
    address payable public buyer;
    address payable public seller;
    address public arbiter;
    
    uint public price;
    
    constructor(address payable _buyer, address payable _seller, address _arbiter, uint _price) {
        buyer = _buyer;
        seller = _seller;
        arbiter = _arbiter;
        price = _price;
    }
    
    function pay() external payable {
        require(phase == 2 && msg.sender == buyer, "access control or flow guard failed");
        require(msg.value == price, "Wrong amount");
        phase = 1;
    }
    
    function  confirmDelivery() public {
        require(phase == 1 && msg.sender == buyer, "access control or flow guard failed");
        seller.transfer(address(this).balance);
        phase = 0;
    }
    
    function arbitrate(uint8 _back) public {
        require(phase == 1 && msg.sender == arbiter, "access control or flow guard failed");
        
        if (_back == 1){
            buyer.transfer(address(this).balance);
        } else {
            seller.transfer(address(this).balance);
        }
        phase = 0;
    }
}
