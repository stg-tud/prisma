// SPDX-License-Identifier: UNLICENSED
pragma solidity >=0.7.0;

contract ManualCrowdfunding {
    
    address payable public owner;
    uint public goal;
    uint public deadline;
    
    mapping(address => uint) private fundings;
    
    constructor(uint _goal, uint _deadline){
        
        goal = _goal;
        deadline = _deadline;
        owner = payable(msg.sender);
        
    }
    
    function fund() public payable {
        
        require(block.timestamp <= deadline, "To late to fund");
        
        fundings[msg.sender] += msg.value;
    }
    
    function claim() public {
    	
    	require(block.timestamp  > deadline, "To early to get money out");
    	require(address(this).balance >= goal, "Goal not reached");
    	owner.transfer(address(this).balance);
    }
    
    function refund() public {
    	require(block.timestamp  > deadline, "To early to get money out");
    	require(address(this).balance < goal, "Goal not reached");
	payable(msg.sender).transfer(fundings[msg.sender]);
	fundings[msg.sender] = 0;
    }
}
