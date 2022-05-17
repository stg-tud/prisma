// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract CompiledRPS  {
  struct Opening {
    uint8 rps1;
    uint256 nonce;
  } 
  address payable[] private players;
  uint256 public winner;
  uint8 public rps2;
  uint256 public commitment;
  uint256 public deadline;
  function open(Opening memory decommitment)  public {
    require((state == uint32(0x902c83b9)), "access control or flow guard failed");
    if ((deadline > block.timestamp)) {
      require((payable(msg.sender) == players[uint256(0)]), "Wrong party to open");
      require((commitment == uint256(keccak256(abi.encode(decommitment)))), "Wrong opening");
      (winner) = (((uint256(decommitment.rps1) % uint256(3)) == (uint256(rps2) % uint256(3))) ? uint256(3) : (((uint256(decommitment.rps1) % uint256(3)) == ((uint256(rps2) + uint256(1)) % uint256(3))) ? uint256(1) : uint256(2)));
      uint8(0);
    } else {
      (winner) = uint256(2);
    } 
    (state) = 0;
  } 
  function move(uint8 tmp)  public {
    require(((state == uint32(0x3c1d1878)) && (players[uint256(1)] == payable(msg.sender))), "access control or flow guard failed");
    (rps2) = tmp;
    (deadline) = (block.timestamp + uint256(10));
    (state) = uint32(0x902c83b9);
  } 
  function commit(uint256 tmp)  public {
    require(((state == uint32(0xacc83c6b)) && (players[uint256(0)] == payable(msg.sender))), "access control or flow guard failed");
    (commitment) = tmp;
    (state) = uint32(0x3c1d1878);
  } 
  constructor(address payable[] memory players$) {
    players = players$;
    winner = uint256(0);
    rps2 = uint8(0);
    commitment = uint256(0);
    deadline = uint256(0);
    (state) = uint32(0xacc83c6b);
    uint8(0);
    
  } 
  function get_players()  public view returns (address payable[] memory result) {
    address payable[] memory tmp = players;
    return(tmp);
  } 
  
  uint32 public state;
  
  
  
  
  

} 