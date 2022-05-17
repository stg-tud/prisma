// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract CompiledChineseCheckers  {
  address payable[] private players;
  struct Move {
    uint256 root;
    uint8[] directions;
  } 
  struct StepResult {
    uint256 pos;
    bool leaped;
  } 
  struct Coordinate {
    uint256 dir;
    uint256 length1;
    uint256 length2;
  } 
  uint8[121] private board;
  uint256 public moves;
  function goalCheck(uint256 _player)  private returns (bool) {
    uint256 checkDirection = uint256(0);
    bool _win = true;
    if ((_player == uint256(1))) {
      (checkDirection) = uint256(3);
    } else if ((_player == uint256(2))) {
      (checkDirection) = uint256(5);
    } else {
      (checkDirection) = uint256(1);
    } 
    uint256 b = uint256(4);
    while (true) {
      if (!((b >= uint256(1)))) {
        break;
      } 
      uint256 d = ((uint256(5) - b) - uint256(1));
      while (true) {
        if (!((d <= uint256(4)))) {
          break;
        } 
        if ((board[coordinatesToIndex(checkDirection, b, d)] != _player)) {
          (_win) = false;
          (d) = uint256(4);
          (b) = uint256(1);
        } else {
          uint8(0);
        } 
        (d) = (d + uint256(1));
      } 
      (b) = (b - uint256(1));
    } 
    if ((_win && (board[coordinatesToIndex(((checkDirection + uint256(1)) % uint256(6)), uint256(4), uint256(0))] != _player))) {
      (_win) = false;
    } else {
      uint8(0);
    } 
    return(_win);
  } 
  function testStep(uint256 _pos, uint8 _dir)  private returns (StepResult memory) {
    uint256 _newPos = getHopTarget(_pos, uint256(_dir));
    bool _leaped = false;
    if (((_newPos != uint256(122)) && (board[_newPos] != uint8(0)))) {
      (_newPos) = getHopTarget(_newPos, uint256(_dir));
      if (((_newPos != uint256(122)) && (board[_newPos] != uint8(0)))) {
        (_newPos) = uint256(122);
      } else {
        (_leaped) = true;
      } 
    } else {
      uint8(0);
    } 
    return(StepResult(_newPos, _leaped));
  } 
  function getHopTarget(uint256 _pos, uint256 _dir)  private returns (uint256) {
    Coordinate memory _pre = indexToCoordinate(_pos);
    if ((_dir > uint256(5))) {
      return(uint256(122));
    } else if ((((_pre.dir + _pre.length1) + _pre.length2) == uint256(0))) {
      return(coordinatesToIndex(_dir, uint256(1), uint256(0)));
    } else if ((_dir == _pre.dir)) {
      return(coordinatesToIndexChecked(_pre.dir, (_pre.length1 + uint256(1)), _pre.length2));
    } else if ((_dir == ((_pre.dir + uint256(1)) % uint256(6)))) {
      return(coordinatesToIndexChecked(_pre.dir, _pre.length1, (_pre.length2 + uint256(1))));
    } else if ((_dir == ((_pre.dir + uint256(2)) % uint256(6)))) {
      if ((_pre.length1 == uint256(1))) {
        return(coordinatesToIndexChecked(((_pre.dir + uint256(1)) % uint256(6)), (_pre.length2 + uint256(1)), uint256(0)));
      } else {
        return(coordinatesToIndexChecked(_pre.dir, (_pre.length1 - uint256(1)), (_pre.length2 + uint256(1))));
      } 
    } else if ((_dir == ((_pre.dir + uint256(3)) % uint256(6)))) {
      if ((_pre.length1 == uint256(1))) {
        if ((_pre.length2 == uint256(0))) {
          return(uint256(0));
        } else {
          return(coordinatesToIndex(((_pre.dir + uint256(1)) % uint256(6)), _pre.length2, uint256(0)));
        } 
      } else {
        return(coordinatesToIndex(_pre.dir, (_pre.length1 - uint256(1)), _pre.length2));
      } 
    } else if ((_dir == ((_pre.dir + uint256(4)) % uint256(6)))) {
      if ((_pre.length2 == uint256(0))) {
        return(coordinatesToIndex(((_pre.dir + uint256(5)) % uint256(6)), uint256(1), (_pre.length1 - uint256(1))));
      } else {
        return(coordinatesToIndex(_pre.dir, _pre.length1, (_pre.length2 - uint256(1))));
      } 
    } else if ((_dir == ((_pre.dir + uint256(5)) % uint256(6)))) {
      if ((_pre.length2 == uint256(0))) {
        return(coordinatesToIndex(((_pre.dir + uint256(5)) % uint256(6)), uint256(1), _pre.length1));
      } else {
        return(coordinatesToIndex(_pre.dir, (_pre.length1 + uint256(1)), (_pre.length2 - uint256(1))));
      } 
    } else {
      return(uint256(122));
    } 
  } 
  function indexToCoordinate(uint256 _index)  private returns (Coordinate memory) {
    require((_index < uint256(121)));
    if ((_index == uint256(0))) {
      return(Coordinate(uint256(0), uint256(0), uint256(0)));
    } else {
      uint256 _length2 = ((_index - uint256(1)) % uint256(5));
      uint256 _length1 = (((((_index - uint256(1)) - _length2) / uint256(5)) % uint256(4)) + uint256(1));
      uint256 _dir = (((_index - uint256(1)) - ((_index - uint256(1)) % uint256(20))) / uint256(20));
      return(Coordinate(_dir, _length1, _length2));
    } 
  } 
  function coordinatesToIndex(uint256 _dir, uint256 _l1, uint256 _l2)  private returns (uint256) {
    return((((_dir * uint256(20)) + ((_l1 - uint256(1)) * uint256(5))) + (_l2 + uint256(1))));
  } 
  function coordinatesToIndexChecked(uint256 _dir, uint256 _l1, uint256 _l2)  private returns (uint256) {
    if (((((_dir > uint256(5)) || (_l1 == uint256(0))) || (_l1 > uint256(4))) || (_l2 > uint256(4)))) {
      revert("Jump to invalid coordinate");
    } else {
      uint8(0);
    } 
    return(coordinatesToIndex(_dir, _l1, _l2));
  } 
  function move(Move memory move)  public {
    require(((state == uint32(0x3c1d1878)) && (players[(moves % uint256(3))] == payable(msg.sender))), "access control or flow guard failed");
    require((board[move.root] == uint8(((moves % uint256(3)) + uint256(1)))), "Not the players token");
    StepResult memory _aux = StepResult(move.root, false);
    uint256 _i = uint256(0);
    while (true) {
      if (!((_i < move.directions.length))) {
        break;
      } 
      (_aux) = testStep(_aux.pos, move.directions[_i]);
      if ((_aux.pos == uint8(122))) {
        revert("Moved out of the board");
      } else {
        uint8(0);
      } 
      if (((_aux.leaped == false) && (move.directions.length != uint256(1)))) {
        revert("No leap but multiple moves");
      } else {
        uint8(0);
      } 
      (_i) = (_i + uint256(1));
    } 
    (board[move.root]) = uint8(0);
    (board[_aux.pos]) = (uint8((moves % uint256(3))) + uint8(1));
    private$whil$macro$3();
  } 
  function private$whil$macro$3()  private {
    if (!(goalCheck(((moves % uint256(3)) + uint256(1))))) {
      (moves) = (moves + uint256(1));
      (state) = uint32(0x3c1d1878);
    } else {
      (state) = 0;
    } 
  } 
  constructor(address payable[] memory players$) {
    players = players$;
    board = [uint8(0), uint8(0), uint8(0), uint8(0), uint8(1), uint8(1), uint8(0), uint8(0), uint8(1), uint8(1), uint8(1), uint8(0), uint8(1), uint8(1), uint8(1), uint8(1), uint8(1), uint8(1), uint8(1), uint8(1), uint8(1), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(1), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(2), uint8(2), uint8(0), uint8(0), uint8(2), uint8(2), uint8(2), uint8(0), uint8(2), uint8(2), uint8(2), uint8(2), uint8(2), uint8(2), uint8(2), uint8(2), uint8(2), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(2), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(3), uint8(3), uint8(0), uint8(0), uint8(3), uint8(3), uint8(3), uint8(0), uint8(3), uint8(3), uint8(3), uint8(3), uint8(3), uint8(3), uint8(3), uint8(3), uint8(3), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(3), uint8(0), uint8(0), uint8(0), uint8(0)];
    moves = uint256(2);
    private$whil$macro$3();
    uint8(0);
    
  } 
  function get_players()  public view returns (address payable[] memory result) {
    address payable[] memory tmp = players;
    return(tmp);
  } 
  function get_board()  public view returns (uint8[121] memory result) {
    uint8[121] memory tmp = board;
    return(tmp);
  } 
  
  uint32 public state;
  
  
  
  
  

} 