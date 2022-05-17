// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract CompiledToken  {
  mapping (address=>uint256) private _balances;
  mapping (address=>mapping (address=>uint256)) private _allowances;
  uint256 private _totalSupply;
  string public name;
  event Transfer(address indexed from, address indexed to, uint256 value);
  event Approval(address indexed owner, address indexed spender, uint256 value);
  function transfer(address recipient, uint256 amount)  public returns (bool) {
    _transfer(payable(msg.sender), recipient, amount);
    return(true);
  } 
  function approve(address spender, uint256 amount)  public returns (bool) {
    _approve(payable(msg.sender), spender, amount);
    return(true);
  } 
  function transferFrom(address sender, address recipient, uint256 amount)  public returns (bool) {
    _transfer(sender, recipient, amount);
    uint256 currentAllowance = _allowances[sender][payable(msg.sender)];
    require((currentAllowance >= amount), "ERC20: transfer amount exceeds allowance");
    _approve(sender, payable(msg.sender), (currentAllowance - amount));
    return(true);
  } 
  function _transfer(address sender, address recipient, uint256 amount)  private {
    uint256 senderBalance = _balances[sender];
    require((senderBalance >= amount), "ERC20: transfer amount exceeds balance");
    (_balances[sender]) = (senderBalance - amount);
    (_balances[recipient]) = (_balances[recipient] + amount);
    emit Transfer(sender, recipient, amount);
  } 
  function _approve(address owner, address spender, uint256 amount)  private {
    (_allowances[owner][spender]) = amount;
    emit Approval(owner, spender, amount);
  } 
  function allowance(address owner, address spender)  view public returns (uint256) {
    return(_allowances[owner][spender]);
  } 
  function totalSupply()  view public returns (uint256) {
    return(_totalSupply);
  } 
  function balanceOf(address account)  view public returns (uint256) {
    return(_balances[account]);
  } 
  constructor(uint256 initialSupply$) {
    _totalSupply = initialSupply$;
    name = "Prisma-Token";
    (_balances[payable(msg.sender)]) = _totalSupply;
    uint8(0);
    
  } 
  
  
  
  
  
  
  

} 