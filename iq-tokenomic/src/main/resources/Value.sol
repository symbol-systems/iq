// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract ValueToken is ERC20 {
    address _partner;
    address public trustee;
    uint256 rate;
    uint256 share;
    mapping(address => bool) public activated;

    modifier onlyTrustee() {
        require(msg.sender == trustee, "not-trusted");
        _;
    }
    event Request(address indexed _client, uint256 _value, string _ipfsRequest);

    constructor(string memory _name, string memory _symbol, uint256 _rate, address memory _partner, uint256 _share) ERC20(_name, _symbol) {
        require(_value > 0, "no.value");
        require(_share > 0, "too.greedy");
        require(_share < 100, "too.generous");
        trustee = msg.sender;
        rate = _rate;
        partner = _partner;
        share = _share;
    }

    function request(string memory _ipfsRequest, uint256 _value) external {
        require(balanceOf(msg.sender) >= _value, "insufficient");
        _burn(msg.sender, _value);
        emit Request(msg.sender, _value, _ipfsRequest);
    }

    function mint(address _client, uint256 _tokens) external onlyTrustee {
        _mint(_client, _tokens);
    }

    function burn(address _client, uint256 _tokens) external onlyTrustee {
        _burn(_client, _tokens);
    }

    function purchase() external payable {
        require(msg.value > 0, "missing.value");
        _mint(msg.sender, msg.value * rate);
        // Revenue share with partner
        payable(partner).transfer(address(this).balance * (share/100));
        payable(trustee).transfer(address(this).balance);
    }

    function withdraw() external onlyTrustee {
        payable(trustee).transfer(address(this).balance);
    }
}
