// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract CompiledMultiSig  {
  struct Sig {
    uint8 v;
    uint256 r;
    uint256 s;
  } 
  struct Execution {
    Sig[] signatures;
    uint256[] indices;
    uint256 timeout;
    address payable destination;
    uint256 value;
    bytes data;
  } 
  address payable[] private parties;
  uint256 public threshold;
  uint256 public nonce;
  receive() external payable  {
    uint8(0);
  } 
  function execute(Execution memory request)  public {
    require((request.signatures.length == threshold), "Wrong number of signatures");
    require((request.indices.length == threshold), "Wrong number of indices");
    require((block.timestamp < request.timeout), "Too late to submit");
    require((address(this).balance >= request.value), "Not enough coins");
    uint256 theHash = uint256(keccak256(abi.encodePacked(address(this), nonce, request.timeout, request.destination, request.value, request.data)));
    address last = payable(0);
    uint256 i = uint256(0);
    while (true) {
      if (!((i < threshold))) {
        break;
      } 
      address rec = ecrecover(bytes32(uint256(keccak256(abi.encodePacked("\x19Ethereum Signed Message:\n32", theHash)))), request.signatures[i].v, bytes32(request.signatures[i].r), bytes32(request.signatures[i].s));
      require((rec > last), "Wrong ordering");
      require((rec == parties[request.indices[i]]), "Wrong signature");
      (last) = rec;
      (i) = (i + uint256(1));
    } 
    (nonce) = (nonce + uint256(1));
    (bool success, bytes memory data) = request.destination.call{value:request.value}(request.data);
    require(success, "Call failed");
  } 
  constructor(uint256 threshold$, address payable[] memory parties$) {
    parties = parties$;
    threshold = threshold$;
    nonce = uint256(0);
    
  } 
  function get_parties()  public view returns (address payable[] memory result) {
    address payable[] memory tmp = parties;
    return(tmp);
  } 
  
  
  
  
  
  
  

} 