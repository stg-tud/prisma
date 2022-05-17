// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract Test1  {
  
  event Ev();
  
  function modify() public {
  	emit Ev();
  }
  
  function modifyTwo() public {
  	emit Ev();
  }
  
  constructor() {
  }

}


contract Test2  {
  
  event Ev();
  uint state;
  
  function modify() public {
  	emit Ev();
  }
  
  function modifyTwo() public {
  	emit Ev();
  }
  
  constructor() {
      state = 0x3c1d1878;
  }

}


contract Test3  {
  
  event Ev();
  uint state;
  
  function modify() public {
  	emit Ev();
  	state = 0x33ca83cb;
  }
  
  function modifyTwo() public {
  	emit Ev();
  	state = 0x902c83b9;
  }
  
  constructor() {
      state = 0x3c1d1878;
  }

}

contract Test4  {
  
  event Ev();
  uint public state;
  
  function modify() public {
  	emit Ev();
  	state = 0x33ca83cb;
  }
  
  function modifyTwo() public {
  	emit Ev();
  	state = 0x902c83b9;
  }
  
  constructor() {
      state = 0x3c1d1878;
  }

}









