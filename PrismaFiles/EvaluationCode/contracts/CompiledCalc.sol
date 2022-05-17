// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract CompiledCalc  {
  struct U8Pair {
    uint8 a;
    uint8 b;
  } 
  struct U8PairPair {
    U8Pair a;
    U8Pair b;
  } 
  uint16 public result;
  function ttA()  public returns (uint8[] memory) {
    uint8[] memory tmp$macro$15 = new uint8[](uint256(2));
    (tmp$macro$15[uint256(0)]) = uint8(0x12);
    (tmp$macro$15[uint256(1)]) = uint8(0x34);
    return(tmp$macro$15);
  } 
  function ttAA()  public returns (uint8[][] memory) {
    uint8[][] memory tmp$macro$16 = new uint8[][](uint256(3));
    uint8[] memory tmp$macro$17 = new uint8[](uint256(2));
    (tmp$macro$17[uint256(0)]) = uint8(0x12);
    (tmp$macro$17[uint256(1)]) = uint8(0x34);
    (tmp$macro$16[uint256(0)]) = tmp$macro$17;
    uint8[] memory tmp$macro$18 = new uint8[](uint256(2));
    (tmp$macro$18[uint256(0)]) = uint8(0x56);
    (tmp$macro$18[uint256(1)]) = uint8(0x12);
    (tmp$macro$16[uint256(1)]) = tmp$macro$18;
    uint8[] memory tmp$macro$19 = new uint8[](uint256(2));
    (tmp$macro$19[uint256(0)]) = uint8(0x34);
    (tmp$macro$19[uint256(1)]) = uint8(0x56);
    (tmp$macro$16[uint256(2)]) = tmp$macro$19;
    return(tmp$macro$16);
  } 
  function co$add$macro$1(uint16 n)  public {
    (result) = (result + n);
    mk(Closure(0x0, abi.encode(result)));
  } 
  function co$sub$macro$2(uint16 n)  public {
    (result) = (result - n);
    mk(Closure(0x0, abi.encode(result)));
  } 
  function co$mul$macro$3(uint16 n)  public {
    (result) = (result * n);
    mk(Closure(0x0, abi.encode(result)));
  } 
  function co$div$macro$4(uint16 n)  public {
    (result) = (result / n);
    mk(Closure(0x0, abi.encode(result)));
  } 
  function co$clear$macro$5()  public {
    (result) = uint16(0);
    mk(Closure(0x0, abi.encode(result)));
  } 
  function co$tu$macro$6(uint256 n)  public {
    mk(Closure(0x0, abi.encode(n)));
  } 
  function co$tu8$macro$7(uint8 n)  public {
    mk(Closure(0x0, abi.encode(n)));
  } 
  function co$tu16$macro$8(uint16 n)  public {
    mk(Closure(0x0, abi.encode(n)));
  } 
  function co$tu32$macro$9(uint32 n)  public {
    mk(Closure(0x0, abi.encode(n)));
  } 
  function co$tu64$macro$10(uint64 n)  public {
    mk(Closure(0x0, abi.encode(n)));
  } 
  function co$tP$macro$11()  public {
    mk(Closure(0x0, abi.encode(U8Pair(uint8(0x12), uint8(0x34)))));
  } 
  function co$tPP$macro$12()  public {
    mk(Closure(0x0, abi.encode(U8PairPair(U8Pair(uint8(0x12), uint8(0x34)), U8Pair(uint8(0x56), uint8(0x78))))));
  } 
  function co$tA$macro$13()  public {
    uint8[] memory tmp$macro$20 = new uint8[](uint256(2));
    (tmp$macro$20[uint256(0)]) = uint8(0x12);
    (tmp$macro$20[uint256(1)]) = uint8(0x34);
    mk(Closure(0x0, abi.encode(tmp$macro$20)));
  } 
  function co$tAA$macro$14()  public {
    uint8[][] memory tmp$macro$21 = new uint8[][](uint256(3));
    uint8[] memory tmp$macro$22 = new uint8[](uint256(2));
    (tmp$macro$22[uint256(0)]) = uint8(0x12);
    (tmp$macro$22[uint256(1)]) = uint8(0x34);
    (tmp$macro$21[uint256(0)]) = tmp$macro$22;
    uint8[] memory tmp$macro$23 = new uint8[](uint256(2));
    (tmp$macro$23[uint256(0)]) = uint8(0x56);
    (tmp$macro$23[uint256(1)]) = uint8(0x12);
    (tmp$macro$21[uint256(1)]) = tmp$macro$23;
    uint8[] memory tmp$macro$24 = new uint8[](uint256(2));
    (tmp$macro$24[uint256(0)]) = uint8(0x34);
    (tmp$macro$24[uint256(1)]) = uint8(0x56);
    (tmp$macro$21[uint256(2)]) = tmp$macro$24;
    mk(Closure(0x0, abi.encode(tmp$macro$21)));
  } 
  constructor() {
    result = uint16(0);
    
  } 
  
  uint32 public state;
  bytes public reqArgs;
  
  struct Closure { uint32 id; bytes data; }
  
  function mk(Closure memory response) private {
    reqArgs = response.data;
    
    state = 0;
  }
  
  function flatMap(Closure memory request, Closure memory callback) private {
    reqArgs = request.data;
    
    state = callback.id;
  }

} 