// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract CompiledSum  {
  uint256 public times;
  string public result;
  function resultlen()  public returns (uint256) {
    return(bytes(result).length);
  } 
  struct placet {
    string theName;
  } 
  function place(string memory thePlace)  public {
    placet memory userdata = abi.decode(store, (placet));
    string memory theName = userdata.theName;
    require((state == uint32(0xd01bc09b)), "access control or flow guard failed");
    private$whil$macro$2(theName, thePlace);
  } 
  function name(string memory theName)  public {
    require((state == uint32(0xaeb3f2a0)), "access control or flow guard failed");
    flatMap(Closure(0xd01bc09b, abi.encode()), Closure(uint32(0xd01bc09b),  hex"" ));
  } 
  struct name2t {
    string thePlace;
    string theName;
  } 
  function name2(string memory tmp)  public {
    name2t memory userdata = abi.decode(store, (name2t));
    string memory thePlace = userdata.thePlace;
    string memory theName = userdata.theName;
    require((state == uint32(0x172b5e7d)), "access control or flow guard failed");
    private$whil$macro$2(theName, thePlace);
  } 
  function private$whil$macro$2(string memory theName, string memory thePlace)  private {
    if ((times > uint256(0))) {
      (times) = (times - uint256(1));
      flatMap(Closure(0x172b5e7d, abi.encode()), Closure(uint32(0x172b5e7d),  hex"" ));
    } else {
      (result) = string(abi.encodePacked(string(abi.encodePacked(string(abi.encodePacked(string(abi.encodePacked(string(abi.encodePacked("hello ", thePlace)), " ")), theName)), " ")), thePlace));
      mk(Closure(0x0, abi.encode(uint8(0))));
    } 
  } 
  constructor() {
    times = uint256(2);
    result = "holululu";
    flatMap(Closure(0xaeb3f2a0, abi.encode()), Closure(uint32(0xaeb3f2a0),  hex"" ));
    uint8(0);
    
  } 
  
  uint32 public state;
  
  bytes public store;
  struct Closure { uint32 id; bytes data; }
  
  function mk(Closure memory response) private {
    
    store = hex"";
    state = 0;
  }
  
  function flatMap(Closure memory request, Closure memory callback) private {
    
    store = callback.data;
    state = callback.id;
  }

} 