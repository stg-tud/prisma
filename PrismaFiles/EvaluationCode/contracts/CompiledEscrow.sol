// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract CompiledEscrow  {
  address payable private buyer;
  address payable private seller;
  address payable private arbiter;
  uint256 public price;
  function proceed(uint8 sendBack)  public {
    require((state == uint32(0x0f218c7c)), "access control or flow guard failed");
    if (((payable(msg.sender) == buyer) || ((payable(msg.sender) == arbiter) && (sendBack == uint8(0))))) {
      seller.transfer(address(this).balance);
    } else if ((payable(msg.sender) == arbiter)) {
      buyer.transfer(address(this).balance);
    } else {
      revert("Wrong party");
    } 
    (state) = 0;
  } 
  function pay_payable(uint8 dummy)  public payable {
    require(((state == uint32(0xe745506e)) && (buyer == payable(msg.sender))), "access control or flow guard failed");
    require((msg.value == price), "Wrong amount");
    (state) = uint32(0x0f218c7c);
  } 
  constructor(address payable buyer$, address payable seller$, address payable arbiter$, uint256 price$) {
    buyer = buyer$;
    seller = seller$;
    arbiter = arbiter$;
    price = price$;
    (state) = uint32(0xe745506e);
    uint8(0);
    
  } 
  function get_buyer()  public view returns (address payable result) {
    address payable tmp = buyer;
    return(tmp);
  } 
  function get_seller()  public view returns (address payable result) {
    address payable tmp = seller;
    return(tmp);
  } 
  function get_arbiter()  public view returns (address payable result) {
    address payable tmp = arbiter;
    return(tmp);
  } 
  
  uint32 public state;
  
  
  
  
  

} 