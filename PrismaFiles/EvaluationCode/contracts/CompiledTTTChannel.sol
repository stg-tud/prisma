// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract CompiledTTTChannel  {
  struct State {
    uint256 version;
    uint8 moves;
    uint8[][] board;
  } 
  struct Sig {
    uint8 v;
    uint256 r;
    uint256 s;
  } 
  struct SignedState {
    State state;
    Sig sig1;
    Sig sig2;
  } 
  struct UU {
    uint8 x;
    uint8 y;
  } 
  address payable[] private parties;
  State private onChain;
  uint256 public timeout;
  function theDispute(SignedState memory cp)  private {
    require((cp.state.version > onChain.version), "Old version");
    uint256 theHash = uint256(keccak256(abi.encode(State(cp.state.version, cp.state.moves, cp.state.board))));
    require((ecrecover(bytes32(uint256(keccak256(abi.encodePacked("\x19Ethereum Signed Message:\n32", theHash)))), cp.sig1.v, bytes32(cp.sig1.r), bytes32(cp.sig1.s)) == parties[uint256(0)]), "Wrong P-0 signature");
    require((ecrecover(bytes32(uint256(keccak256(abi.encodePacked("\x19Ethereum Signed Message:\n32", theHash)))), cp.sig2.v, bytes32(cp.sig2.r), bytes32(cp.sig2.s)) == parties[uint256(1)]), "Wrong P-1 signature");
    if ((timeout == uint256(0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff))) {
      (timeout) = (block.timestamp + uint256(10));
    } else {
      uint8(0);
    } 
    (onChain) = cp.state;
  } 
  function public$won(uint8 x, uint8 y, uint8 pos, uint8[][] memory board)  private returns (bool) {
    return(((((board[uint256(x)][uint256(0)] == board[uint256(x)][uint256(1)]) && (board[uint256(x)][uint256(1)] == board[uint256(x)][uint256(2)])) || ((board[uint256(0)][uint256(y)] == board[uint256(1)][uint256(y)]) && (board[uint256(1)][uint256(y)] == board[uint256(2)][uint256(y)]))) || ((board[uint256(1)][uint256(1)] == pos) && (((board[uint256(0)][uint256(2)] == board[uint256(1)][uint256(1)]) && (board[uint256(2)][uint256(0)] == board[uint256(1)][uint256(1)])) || ((board[uint256(0)][uint256(0)] == board[uint256(1)][uint256(1)]) && (board[uint256(2)][uint256(2)] == board[uint256(1)][uint256(1)]))))));
  } 
  function theMove(UU memory pair)  private {
    require((onChain.board[uint256(pair.x)][uint256(pair.y)] == uint8(0)), "Field already occupied");
    uint8 pos = ((onChain.moves % uint8(2)) + uint8(2));
    (onChain.board[uint256(pair.x)][uint256(pair.y)]) = pos;
    bool cond = public$won(pair.x, pair.y, pos, onChain.board);
    (onChain.moves) = (cond ? (uint8(10) + pos) : (onChain.moves + uint8(1)));
  } 
  function move(UU memory incoming)  public {
    require(((state == uint32(0x3c1d1878)) && (parties[uint256((onChain.moves % uint8(2)))] == payable(msg.sender))), "access control or flow guard failed");
    theMove(incoming);
    private$whil$macro$6();
  } 
  function private$whil$macro$6()  private {
    if ((onChain.moves < uint8(9))) {
      (state) = uint32(0x3c1d1878);
    } else {
      (state) = 0;
    } 
  } 
  function dispute(SignedState memory latestState)  public {
    require((state == uint32(0x5f27452d)), "access control or flow guard failed");
    if ((timeout > block.timestamp)) {
      theDispute(latestState);
    } else {
      theMove(UU(latestState.sig1.v, latestState.sig2.v));
    } 
    private$whil$macro$5();
  } 
  function private$whil$macro$5()  private {
    if ((timeout > block.timestamp)) {
      (state) = uint32(0x5f27452d);
    } else {
      private$whil$macro$6();
    } 
  } 
  constructor(address payable[] memory parties$) {
    parties = parties$;
    uint8[][] memory tmp$macro$7 = new uint8[][](uint256(3));
    uint8[] memory tmp$macro$8 = new uint8[](uint256(3));
    (tmp$macro$8[uint256(0)]) = uint8(0);
    (tmp$macro$8[uint256(1)]) = uint8(0);
    (tmp$macro$8[uint256(2)]) = uint8(0);
    (tmp$macro$7[uint256(0)]) = tmp$macro$8;
    uint8[] memory tmp$macro$9 = new uint8[](uint256(3));
    (tmp$macro$9[uint256(0)]) = uint8(0);
    (tmp$macro$9[uint256(1)]) = uint8(0);
    (tmp$macro$9[uint256(2)]) = uint8(0);
    (tmp$macro$7[uint256(1)]) = tmp$macro$9;
    uint8[] memory tmp$macro$10 = new uint8[](uint256(3));
    (tmp$macro$10[uint256(0)]) = uint8(0);
    (tmp$macro$10[uint256(1)]) = uint8(0);
    (tmp$macro$10[uint256(2)]) = uint8(0);
    (tmp$macro$7[uint256(2)]) = tmp$macro$10;
    onChain = State(uint256(0), uint8(0), tmp$macro$7);
    timeout = uint256(0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff);
    private$whil$macro$5();
    uint8(0);
    
  } 
  function get_parties()  public view returns (address payable[] memory result) {
    address payable[] memory tmp = parties;
    return(tmp);
  } 
  function get_onChain()  public view returns (State memory result) {
    State memory tmp = onChain;
    return(tmp);
  } 
  
  uint32 public state;
  
  
  
  
  

} 