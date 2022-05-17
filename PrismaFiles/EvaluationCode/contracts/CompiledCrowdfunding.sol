// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract CompiledCrowdfunding  {
  uint256 public goal;
  uint256 public deadline;
  address payable private owner;
  mapping (address=>uint256) private fundings;
  function allInOne_payable(uint256 dummy)  public payable {
    require((state == uint32(0x5fef5000)), "access control or flow guard failed");
    if ((deadline > block.timestamp)) {
      (fundings[payable(msg.sender)]) = (fundings[payable(msg.sender)] + msg.value);
    } else if ((address(this).balance >= goal)) {
      owner.transfer(address(this).balance);
    } else {
      payable(msg.sender).transfer(fundings[payable(msg.sender)]);
      (fundings[payable(msg.sender)]) = uint256(0);
    } 
    private$whil$macro$3();
  } 
  function private$whil$macro$3()  private {
    if (((deadline > block.timestamp) || (address(this).balance > uint256(0)))) {
      (state) = uint32(0x5fef5000);
    } else {
      (state) = 0;
    } 
  } 
  constructor(uint256 goal$, uint256 deadline$) {
    goal = goal$;
    deadline = deadline$;
    owner = payable(msg.sender);
    private$whil$macro$3();
    uint8(0);
    
  } 
  function get_owner()  public view returns (address payable result) {
    address payable tmp = owner;
    return(tmp);
  } 
  
  uint32 public state;
  
  
  
  
  

} 