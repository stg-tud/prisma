// SPDX-License-Identifier: UNLICENSED
pragma solidity >=0.7.0;
pragma experimental ABIEncoderV2;

contract ManualTTTChannel {
    
    struct State {
        uint256 version;
        uint8 moves;
        uint8[][] board;
    }
    
    struct Sig {
        uint8 v;
        bytes32 r;
        bytes32 s;
    }
  
    address [] parties;
    
    uint public timeout = 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff;
    
    State private onchain;

    constructor(address[] memory _parties) public {
        parties = _parties;
        onchain.version = 0;
        onchain.moves = 0;
        onchain.board = [[0,0,0],[0,0,0],[0,0,0]];
      }
    
    function dispute(State memory _state, Sig memory sig1, Sig memory sig2) public {
	    
            require(_state.version > onchain.version, "Old version");
            require(block.timestamp < timeout, "Timeout elapsed");

            bytes32 _hash = keccak256(abi.encodePacked("\x19Ethereum Signed Message:\n32", keccak256(abi.encode(_state))));

            require(ecrecover(_hash, sig1.v, sig1.r, sig1.s) == parties[0], "Wrong P-0 signature");
            require(ecrecover(_hash, sig2.v, sig2.r, sig2.s) == parties[1], "Wrong P-1 signature");

            onchain = _state;

            if (timeout == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff)
             timeout = block.timestamp + 10;


        }

    function move(uint8 x, uint8 y) external  {

        require(block.timestamp > timeout, "Dispute period is not over");

        uint8 pos = onchain.moves % 2;
        require(onchain.moves < 9, "Game has already terinated");
        require(msg.sender == parties[pos], "Not the right player to make the move");
        require(onchain.board[x][y] == 0, "Invalid move");
        onchain.board[x][y] = pos + 2;
        bool cond = won(x, y, pos, onchain.board);
    	onchain.moves = cond ? 10 + pos : onchain.moves + 1;
    }
    
    function won(uint8 x, uint8 y, uint8 pos, uint8[][] memory board) private pure returns(bool){
    	return (board[x][0] == board[x][1] && board[x][1] == board[x][2]
        ||  board[0][y] == board[1][y] && board[1][y] == board[2][y]
        || board[1][1] != 0 && (
        	  board[0][2] == board[1][1] && board[1][1] == board[2][0]
        	  ||  board[0][0] == board[1][1] && board[1][1] == board[2][2] ));
    }
    
    function getOnchain() public view returns(State memory){
        return onchain;
    }

    function getParties() public view returns(address[] memory){
        return parties;
    }
}
