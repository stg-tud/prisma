// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract CompiledHangman  {
  address payable[] private players;
  struct UU {
    uint256 x;
    uint256 y;
  } 
  struct UArrU {
    uint8[] arr;
    uint256 x;
  } 
  uint256 public hashedWord;
  uint8[26] private guessed;
  uint8[] private word;
  uint8 public currentGuess;
  uint256 public missingletters;
  uint256 public tries;
  function commit2(uint256 hash, uint256 wordLength)  private {
    (hashedWord) = hash;
    (missingletters) = wordLength;
    uint256 i = uint256(0);
    while (true) {
      if (!((i < wordLength))) {
        break;
      } 
      word.push(uint8(0));
      (i) = (i + uint256(1));
    } 
  } 
  function guess2(uint8 letter)  private {
    require(((letter - uint8(65)) < uint8(26)), "Not a valid letter");
    require((guessed[uint256((letter - uint8(65)))] == uint8(0)), "Letter has already been guessed");
    (currentGuess) = letter;
  } 
  function respond2(uint256[] memory hits)  private {
    uint256 i = uint256(0);
    while (true) {
      if (!((i < hits.length))) {
        break;
      } 
      require((hits[i] < word.length));
      require((word[hits[i]] == uint8(0)), "this letter was hit before");
      (word[hits[i]]) = currentGuess;
      (i) = (i + uint256(1));
    } 
    (guessed[uint256((currentGuess - uint8(65)))]) = uint8(1);
    if ((hits.length == uint256(0))) {
      (tries) = (tries - uint256(1));
    } else {
      (missingletters) = (missingletters - hits.length);
    } 
  } 
  function open2(UArrU memory opening)  private {
    require((opening.arr.length == word.length), "needs word len letters");
    uint256 i = uint256(0);
    while (true) {
      if (!((i < word.length))) {
        break;
      } 
      if ((((opening.arr[i] - uint8(65)) >= uint8(26)) || ((guessed[uint256((opening.arr[i] - uint8(65)))] == uint8(1)) && !((word[i] == opening.arr[i]))))) {
        (missingletters) = uint256(0);
      } else {
        uint8(0);
      } 
      (i) = (i + uint256(1));
    } 
    if ((uint256(keccak256(abi.encode(opening))) != hashedWord)) {
      (missingletters) = uint256(0);
    } else {
      uint8(0);
    } 
  } 
  function commit(UU memory pair)  public {
    require(((state == uint32(0xacc83c6b)) && (players[uint256(0)] == payable(msg.sender))), "access control or flow guard failed");
    commit2(pair.x, pair.y);
    private$whil$macro$5();
  } 
  function private$iff$macro$6()  private {
    (state) = 0;
  } 
  function respond(uint256[] memory tmp)  public {
    require(((state == uint32(0x111fea59)) && (players[uint256(0)] == payable(msg.sender))), "access control or flow guard failed");
    respond2(tmp);
    private$whil$macro$5();
  } 
  function guess(uint8 tmp)  public {
    require(((state == uint32(0x33ca83cb)) && (players[uint256(1)] == payable(msg.sender))), "access control or flow guard failed");
    guess2(tmp);
    (state) = uint32(0x111fea59);
  } 
  function open(UArrU memory reveal)  public {
    require(((state == uint32(0x902c83b9)) && (players[uint256(0)] == payable(msg.sender))), "access control or flow guard failed");
    open2(reveal);
    private$iff$macro$6();
  } 
  function private$whil$macro$5()  private {
    if (((tries > uint256(0)) && (missingletters != uint256(0)))) {
      (state) = uint32(0x33ca83cb);
    } else if ((missingletters != uint256(0))) {
      (state) = uint32(0x902c83b9);
    } else {
      private$iff$macro$6();
    } 
  } 
  constructor(address payable[] memory players$) {
    players = players$;
    hashedWord = uint256(0);
    guessed = [uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0), uint8(0)];
    uint8[] memory tmp$macro$7 = new uint8[](uint256(0));
    word = tmp$macro$7;
    currentGuess = uint8(0);
    missingletters = uint256(0);
    tries = uint256(5);
    (state) = uint32(0xacc83c6b);
    uint8(0);
    
  } 
  function get_players()  public view returns (address payable[] memory result) {
    address payable[] memory tmp = players;
    return(tmp);
  } 
  function get_guessed()  public view returns (uint8[26] memory result) {
    uint8[26] memory tmp = guessed;
    return(tmp);
  } 
  function get_word()  public view returns (uint8[] memory result) {
    uint8[] memory tmp = word;
    return(tmp);
  } 
  
  uint32 public state;
  
  
  
  
  

} 