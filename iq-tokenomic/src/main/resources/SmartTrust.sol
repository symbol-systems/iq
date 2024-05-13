// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./DAOToken.sol";
import "./DAO.sol";
import "./Value.sol";

/**
 * @title SmartTrust
 * @dev Contract to deploy and initialize DAO contracts and Value Tokens.
 */
contract SmartTrust {
    address public trustee;
    DAOToken public token;
    DAO[] public daos;

    modifier onlyTrustee() {
        require(msg.sender == trustee, "Trustee only");
        _;
    }

    modifier onlyIPFS(string _ipfsHash) {
        require(bytes(_ipfsHash).length == 46, "Invalid IPFS hash"); // Sanity check the IPFS hash
    }

    event ValueOffer(address dao, address valueToken, address trustee, string ipfsOffer);

    /**
     * @dev Constructor function to initialize the SmartTrust.
     * @param _tokenName The name of the DAO.
     * @param _tokenSymbol The symbol of the DAO.
     * @param _initialSupply The initial supply of governance tokens.
     * @param _trustee Address of the DAO trustee.
     * @param _quorum Percentage of voted tokens needed for quorum (1-100).
     * @param _ipfsFoundation The foundation knowledge graph on IPFS.
     */
    constructor(
        string memory _tokenName,
        string memory _tokenSymbol,
        uint256 _initialSupply,
        address _trustee,
        uint8 _quorum,
        string memory _ipfsFoundation
    ) onlyIPFS(_ipfsFoundation) {
        trustee = _trustee;
        // Deploy DAO Token contract
        token = new DAOToken(_tokenName, _tokenSymbol, _trustee, _initialSupply);

        // Launch initial governance contract
        DAO newDAO = new DAO(trustee, address(token), _quorum);
        daos.push(newDAO);
        // Emit the DAO foundation event
        newDAO.observe(_ipfsFoundation);
    }

    /**
     * @dev Function to launch a new DAO.
     * @param _quorum Percentage of voted tokens needed for quorum (1-100).
     * @param _ipfsSpec The IPFS graph of the launch specification.
     */
    function launch(
        uint8 _quorum,
        string memory _ipfsSpec
    ) external onlyTrustee onlyIPFS(_ipfsSpec) {
        // Deploy governance contract for a new DAO
        DAO newDAO = new DAO(trustee, address(token), _quorum);
        daos.push(newDAO);
        // Emit the launch specification event
        newDAO.observe(_ipfsSpec);
        return newDAO;
    }

    /**
     * @dev Function for a DAO to collaborate with a partner on offering value.
     * @param _daoId The id of the new DAO offering the value.
     * @param _valueName The name of the new ValueToken.
     * @param _valueSymbol The symbol of the new ValueToken.
     * @param _rate The exchange rate for the new ValueToken (ValueTokens/Eth).
     * @param _share The % share shared with the partner.
     * @param _ipfsOffer The IPFS hash representing the offer details for the new ValueToken.
     */
    function collab (
        uint256 _daoId,
        string memory _valueName,
        string memory _valueSymbol,
        uint256 _rate,
        address _partner,
        uint256 _share,
        string memory _ipfsOffer
    ) external onlyTrustee onlyIPFS(_ipfsOffer) {
        require(_daoId < daos.length, "Invalid DAO");
        require(_rate>0, "No Value");
        require(_share>0, "No Shared Value");

        DAO dao = daos[_daoId];
        dao.observe(_ipfsOffer);

        ValueToken value = new ValueToken(_valueName, _valueSymbol, _rate, _partner, _share);

        emit ValueOffer(dao, address(value), msg.sender, _ipfsOffer);
        return value;
    }

    /**
     * @dev Function for a DAO to offer value.
     * @param _daoId The id of the new DAO offering the value.
     * @param _valueName The name of the new ValueToken.
     * @param _valueSymbol The symbol of the new ValueToken.
     * @param _rate The exchange rate for the new ValueToken (ValueTokens/Eth).
     * @param _share The % shared with the DAO itself (remainder accrues to the trust).
     * @param _ipfsOffer The IPFS hash representing the offer details for the new ValueToken.
     */
    function offer (
        uint256 _daoId,
        string memory _valueName,
        string memory _valueSymbol,
        uint256 _rate,
        uint256 _share,
        string memory _ipfsOffer
    ) external onlyTrustee onlyIPFS(_ipfsOffer) {
        require(_daoId < daos.length, "Invalid DAO");
        // The DAO retains a share of any value created for operational needs
        return collab(_daoId, _valueName, _valueSymbol, _rate, daos[_daoId], _share);
    }
}
