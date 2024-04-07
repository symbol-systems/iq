// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./I_ERC20.sol";

/**
 * @title GovernanceToken
 * @dev An ERC20 token with additional trustee features.
 */
contract DAOToken is IERC20 {
    address public trustee; // Address of the trustee contract

    /**
     * @dev Constructor function to initialize the GovernanceToken contract.
     * @param _name The name of the token.
     * @param _symbol The symbol of the token.
     * @param _trustee Address of the digital trustee contract.
     * @param _initialSupply The initial supply of tokens.
     */
    constructor(
        string memory _name,
        string memory _symbol,
        address _trustee,
        uint256 _initialSupply
    ) IERC20(_name, _symbol) {
        require(_trustee != address(0), "trustee.invalid");
        trustee = _trustee;
        _mint(msg.sender, _initialSupply);
    }

    /**
     * @dev Modifier to ensure that only the trustee contract can execute certain functions.
     */
    modifier onlyTrustee() {
        require(msg.sender == trustee, "untrusted");
        _;
    }

    /**
     * @dev Mint new tokens.
     * @param _to The address to which new tokens will be minted.
     * @param _amount The amount of tokens to mint.
     */
    function mint(address _to, uint256 _amount) external onlyTrustee {
        _mint(_to, _amount);
    }

    /**
     * @dev Burn tokens from the sender's balance.
     * @param _amount The amount of tokens to burn.
     */
    function burn(uint256 _amount) external {
//        require(msg.sender != trustee, "trustee.safety");
        _burn(msg.sender, _amount);
    }

    /**
     * @dev Burn tokens from a specific account.
     * @param _account The account from which tokens will be burned.
     * @param _amount The amount of tokens to burn.
     */
    function burnFrom(address _account, uint256 _amount) external onlyTrustee {
//        require(_account != trustee, "trustee.safety");
        _burn(_account, _amount);
    }
}
