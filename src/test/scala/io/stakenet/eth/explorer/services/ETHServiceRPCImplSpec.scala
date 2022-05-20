package io.stakenet.eth.explorer.services

import java.math.BigInteger

import com.fasterxml.jackson.databind.ObjectMapper
import io.stakenet.eth.explorer.Helpers
import io.stakenet.eth.explorer.models.{Block, Transaction, TransactionStatus}
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar.{doReturn, mock}
import org.scalactic.Equality
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.{EthBlock, EthBlockNumber, EthGetTransactionReceipt}
import org.web3j.protocol.core.{DefaultBlockParameter, JsonRpc2_0Web3j, Request}

import scala.compat.java8.FutureConverters
import scala.concurrent.Future

class ETHServiceRPCImplSpec extends AsyncWordSpec with Matchers {

  implicit val blockParameterEquality: Equality[DefaultBlockParameter] = (a: DefaultBlockParameter, b: Any) => {
    b match {
      case c: DefaultBlockParameter => a.getValue == c.getValue
      case _ => false
    }
  }

  "getLatestBlockNumber" should {
    "get latest block number" in {
      val ethClient = mock[JsonRpc2_0Web3j]
      val service = getService(ethClient)

      val request = mock[Request[_, EthBlockNumber]]
      val response =
        """{
          |  "jsonrpc": "2.0",
          |  "id": 1,
          |  "result": "0xa9e699"
          |}""".stripMargin
      val blockNumber = new ObjectMapper().readValue(response, classOf[EthBlockNumber])
      doReturn(request).when(ethClient).ethBlockNumber()
      doReturn(FutureConverters.toJava(Future.successful(blockNumber)).toCompletableFuture).when(request).sendAsync()

      service.getLatestBlockNumber().map { result =>
        result mustBe BigInt("11134617")
      }
    }

    "fail when there is not result" in {
      val ethClient = mock[JsonRpc2_0Web3j]
      val service = getService(ethClient)

      val request = mock[Request[_, EthBlock]]
      val response =
        """{
          |  "jsonrpc": "2.0",
          |  "id": 1,
          |  "result": null
          |}""".stripMargin
      val blockNumber = new ObjectMapper().readValue(response, classOf[EthBlockNumber])
      doReturn(request).when(ethClient).ethBlockNumber()
      doReturn(FutureConverters.toJava(Future.successful(blockNumber)).toCompletableFuture).when(request).sendAsync()

      recoverToSucceededIf[ETHService.Error.UnexpectedResponse](service.getLatestBlockNumber())
    }

    "fail when eth node returns an error" in {
      val ethClient = mock[JsonRpc2_0Web3j]
      val service = getService(ethClient)

      val request = mock[Request[_, EthBlock]]
      val response =
        """{
          |  "jsonrpc":"2.0",
          |  "error":{
          |     "code":-32000,
          |     "message":"Requested block number is in a range that is not available yet, because the ancient block sync is still in progress."
          |  },
          |  "id":1
          |}""".stripMargin
      val blockNumber = new ObjectMapper().readValue(response, classOf[EthBlockNumber])
      doReturn(request).when(ethClient).ethBlockNumber()
      doReturn(FutureConverters.toJava(Future.successful(blockNumber)).toCompletableFuture).when(request).sendAsync()

      recoverToSucceededIf[ETHService.Error.CouldNotGetLatestBlockNumber](service.getLatestBlockNumber())
    }
  }

  "getBlock" should {
    "return a block without transactions" in {
      val ethClient = mock[JsonRpc2_0Web3j]
      val service = getService(ethClient)
      val blockNumber = BigInt(123)
      val withTransactions = false

      val request = mock[Request[_, EthBlock]]
      val response =
        """{
          |  "jsonrpc": "2.0",
          |  "id": 1,
          |  "result": {
          |    "difficulty": "0x7d493619822",
          |    "extraData": "0xd983010203844765746887676f312e342e328777696e646f7773",
          |    "gasLimit": "0x2fefd8",
          |    "gasUsed": "0x5208",
          |    "hash": "0xbe9fed17ee6c5009bc4cea8f3f0bc16e88c7d8c8cb86c0c1a123194448ef4052",
          |    "logsBloom": "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
          |    "miner": "0xf8b483dba2c3b7176a3da549ad41a48bb3121069",
          |    "mixHash": "0x1f75357e0173be704fa4b4d5f897629d35e4906564e0cd27af8ad2c065c92dab",
          |    "nonce": "0x8d2223ab586af423",
          |    "number": "0xabcde",
          |    "parentHash": "0x3802a8993a87e33d67de9cc2ea9d53f17dbb50790669f69a3c5a4c2447a38694",
          |    "receiptsRoot": "0x00ded82fdeb31d21a883b1bd63a10de53bbab2f53bd9083371439f282950cec7",
          |    "sha3Uncles": "0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347",
          |    "size": "0x293",
          |    "stateRoot": "0x0376945937ef01fc029ecc88bf83b8e3ddd8b3d3a41e58cdb5cb47f7551003dc",
          |    "timestamp": "0x56722e0b",
          |    "totalDifficulty": "0x3bba26dd90aeb0d9",
          |    "transactions": [
          |      "0xe352a785343d4fcbd5e41a7c9152bae1230365cfe4a7a149310546b60192861f"
          |    ],
          |    "transactionsRoot": "0xb7a599b964485c535cea0567eadee76ce04f9027a90bb6ff82767a2a0ce28566",
          |    "uncles": []
          |  }
          |}""".stripMargin
      val block = new ObjectMapper().readValue(response, classOf[EthBlock])
      val blockParameter = DefaultBlockParameter.valueOf(new BigInteger(blockNumber.toString))
      doReturn(request).when(ethClient).ethGetBlockByNumber(eqTo(blockParameter), eqTo(withTransactions))
      doReturn(FutureConverters.toJava(Future.successful(block)).toCompletableFuture).when(request).sendAsync()

      service.getBlock(blockNumber, withTransactions).map { result =>
        val expected = Block.WithoutTransactions(
          number = 703710,
          hash = "0xbe9fed17ee6c5009bc4cea8f3f0bc16e88c7d8c8cb86c0c1a123194448ef4052",
          parentHash = "0x3802a8993a87e33d67de9cc2ea9d53f17dbb50790669f69a3c5a4c2447a38694",
          nonce = BigInt("10169730127385785379"),
          sha3Uncles = "0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347",
          transactionsRoot = "0xb7a599b964485c535cea0567eadee76ce04f9027a90bb6ff82767a2a0ce28566",
          stateRoot = "0x0376945937ef01fc029ecc88bf83b8e3ddd8b3d3a41e58cdb5cb47f7551003dc",
          receiptRoot = "0x00ded82fdeb31d21a883b1bd63a10de53bbab2f53bd9083371439f282950cec7",
          author = None,
          miner = "0xf8b483dba2c3b7176a3da549ad41a48bb3121069",
          mixHash = "0x1f75357e0173be704fa4b4d5f897629d35e4906564e0cd27af8ad2c065c92dab",
          difficulty = BigInt("8609587107874"),
          totalDifficulty = BigInt("4303795126962925785"),
          extraData = "0xd983010203844765746887676f312e342e328777696e646f7773",
          size = 659,
          gasLimit = 3141592,
          gasUsed = 21000,
          timestamp = 1450323467
        )
        result mustBe expected
      }
    }

    "return a block with transactions" in {
      val ethClient = mock[JsonRpc2_0Web3j]
      val service = getService(ethClient)
      val blockNumber = BigInt(123)
      val withTransactions = false

      val request = mock[Request[_, EthBlock]]
      val receiptRequest = mock[Request[_, EthGetTransactionReceipt]]

      val response =
        """{
          |  "jsonrpc": "2.0",
          |  "id": 1,
          |  "result": {
          |    "difficulty": "0x7d493619822",
          |    "extraData": "0xd983010203844765746887676f312e342e328777696e646f7773",
          |    "gasLimit": "0x2fefd8",
          |    "gasUsed": "0x5208",
          |    "hash": "0xbe9fed17ee6c5009bc4cea8f3f0bc16e88c7d8c8cb86c0c1a123194448ef4052",
          |    "logsBloom": "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
          |    "miner": "0xf8b483dba2c3b7176a3da549ad41a48bb3121069",
          |    "mixHash": "0x1f75357e0173be704fa4b4d5f897629d35e4906564e0cd27af8ad2c065c92dab",
          |    "nonce": "0x8d2223ab586af423",
          |    "number": "0xabcde",
          |    "parentHash": "0x3802a8993a87e33d67de9cc2ea9d53f17dbb50790669f69a3c5a4c2447a38694",
          |    "receiptsRoot": "0x00ded82fdeb31d21a883b1bd63a10de53bbab2f53bd9083371439f282950cec7",
          |    "sha3Uncles": "0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347",
          |    "size": "0x293",
          |    "stateRoot": "0x0376945937ef01fc029ecc88bf83b8e3ddd8b3d3a41e58cdb5cb47f7551003dc",
          |    "timestamp": "0x56722e0b",
          |    "totalDifficulty": "0x3bba26dd90aeb0d9",
          |    "transactions": [
          |      {
          |        "blockHash": "0xbe9fed17ee6c5009bc4cea8f3f0bc16e88c7d8c8cb86c0c1a123194448ef4052",
          |        "blockNumber": "0xabcde",
          |        "from": "0xd1e56c2e765180aa0371928fd4d1e41fbcda34d4",
          |        "gas": "0x15f90",
          |        "gasPrice": "0xba43b7400",
          |        "hash": "0xe352a785343d4fcbd5e41a7c9152bae1230365cfe4a7a149310546b60192861f",
          |        "input": "0x",
          |        "nonce": "0x59b",
          |        "r": "0x132c11aba6c2e71cc513185717de62aa8e2fd00f768f8aaa58803e9a3000ffce",
          |        "s": "0x56424e6f0d53917c8cdbcd007d1ed11f2e69c2c7e49bd40f80d61efac24d33fb",
          |        "to": "0xfabfcdb9ad54126a8829d5ab6357f608c6b005d0",
          |        "transactionIndex": "0x0",
          |        "v": "0x1b",
          |        "value": "0x280b2985d2811a00"
          |      }
          |    ],
          |    "transactionsRoot": "0xb7a599b964485c535cea0567eadee76ce04f9027a90bb6ff82767a2a0ce28566",
          |    "uncles": []
          |  }
          |}""".stripMargin

      val receiptResponse =
        """{
          |  "jsonrpc": "2.0",
          |  "id": 1,
          |  "result": {
          |    "status": "0x1",
          |    "transactionHash": "0xe352a785343d4fcbd5e41a7c9152bae1230365cfe4a7a149310546b60192861f",
          |    "transactionIndex": 0,
          |    "blockHash": "0xef95f2f1ed3ca60b048b4bf67cde2195961e0bba6f70bcbea9a2c4e133e34b46",
          |    "blockNumber": 3,
          |    "contractAddress": "0x11f4d0A3c12e86B4b5F39B213F7E19D048276DAe",
          |    "cumulativeGasUsed": 314159,
          |    "gasUsed": 30234,
          |    "logs": []
          |  }
          |}""".stripMargin

      val block = new ObjectMapper().readValue(response, classOf[EthBlock])
      val blockParameter = DefaultBlockParameter.valueOf(new BigInteger(blockNumber.toString))
      val receipt = new ObjectMapper().readValue(receiptResponse, classOf[EthGetTransactionReceipt])

      doReturn(request).when(ethClient).ethGetBlockByNumber(eqTo(blockParameter), eqTo(withTransactions))
      doReturn(FutureConverters.toJava(Future.successful(block)).toCompletableFuture).when(request).sendAsync()

      doReturn(receiptRequest)
        .when(ethClient)
        .ethGetTransactionReceipt(eqTo("0xe352a785343d4fcbd5e41a7c9152bae1230365cfe4a7a149310546b60192861f"))

      doReturn(FutureConverters.toJava(Future.successful(receipt)).toCompletableFuture).when(receiptRequest).sendAsync()

      service.getBlock(blockNumber, withTransactions).map { result =>
        val transactions = List(
          Transaction(
            hash = "0xe352a785343d4fcbd5e41a7c9152bae1230365cfe4a7a149310546b60192861f",
            nonce = 1435,
            blockHash = "0xbe9fed17ee6c5009bc4cea8f3f0bc16e88c7d8c8cb86c0c1a123194448ef4052",
            blockNumber = 703710,
            transactionIndex = 0,
            from = "0xd1e56c2e765180aa0371928fd4d1e41fbcda34d4",
            to = Some("0xfabfcdb9ad54126a8829d5ab6357f608c6b005d0"),
            value = BigInt("2885445641000000000"),
            gasPrice = BigInt("50000000000"),
            gas = 90000,
            input = "0x",
            creates = None,
            publicKey = None,
            raw = None,
            timestamp = 1450323467,
            Some(TransactionStatus.Success),
            confirmations = 0
          )
        )

        val expected = Block
          .WithoutTransactions(
            number = 703710,
            hash = "0xbe9fed17ee6c5009bc4cea8f3f0bc16e88c7d8c8cb86c0c1a123194448ef4052",
            parentHash = "0x3802a8993a87e33d67de9cc2ea9d53f17dbb50790669f69a3c5a4c2447a38694",
            nonce = BigInt("10169730127385785379"),
            sha3Uncles = "0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347",
            transactionsRoot = "0xb7a599b964485c535cea0567eadee76ce04f9027a90bb6ff82767a2a0ce28566",
            stateRoot = "0x0376945937ef01fc029ecc88bf83b8e3ddd8b3d3a41e58cdb5cb47f7551003dc",
            receiptRoot = "0x00ded82fdeb31d21a883b1bd63a10de53bbab2f53bd9083371439f282950cec7",
            author = None,
            miner = "0xf8b483dba2c3b7176a3da549ad41a48bb3121069",
            mixHash = "0x1f75357e0173be704fa4b4d5f897629d35e4906564e0cd27af8ad2c065c92dab",
            difficulty = BigInt("8609587107874"),
            totalDifficulty = BigInt("4303795126962925785"),
            extraData = "0xd983010203844765746887676f312e342e328777696e646f7773",
            size = 659,
            gasLimit = 3141592,
            gasUsed = 21000,
            timestamp = 1450323467
          )
          .withTransactions(transactions)

        result mustBe expected
      }
    }

    "fail when block does not exist" in {
      val ethClient = mock[JsonRpc2_0Web3j]
      val service = getService(ethClient)
      val blockNumber = BigInt(123)
      val withTransactions = false

      val request = mock[Request[_, EthBlock]]
      val response =
        """{
          |  "jsonrpc": "2.0",
          |  "id": 1,
          |  "result": null
          |}""".stripMargin
      val block = new ObjectMapper().readValue(response, classOf[EthBlock])
      val blockParameter = DefaultBlockParameter.valueOf(new BigInteger(blockNumber.toString))
      doReturn(request).when(ethClient).ethGetBlockByNumber(eqTo(blockParameter), eqTo(withTransactions))
      doReturn(FutureConverters.toJava(Future.successful(block)).toCompletableFuture).when(request).sendAsync()

      recoverToSucceededIf[ETHService.Error.UnexpectedResponse](service.getBlock(blockNumber, withTransactions))
    }

    "fail when eth node returns an error" in {
      val ethClient = mock[JsonRpc2_0Web3j]
      val service = getService(ethClient)
      val blockNumber = BigInt(123)
      val withTransactions = false

      val request = mock[Request[_, EthBlock]]
      val response =
        """{
          |  "jsonrpc":"2.0",
          |  "error":{
          |     "code":-32000,
          |     "message":"Requested block number is in a range that is not available yet, because the ancient block sync is still in progress."
          |  },
          |  "id":1
          |}""".stripMargin
      val block = new ObjectMapper().readValue(response, classOf[EthBlock])
      val blockParameter = DefaultBlockParameter.valueOf(new BigInteger(blockNumber.toString))
      doReturn(request).when(ethClient).ethGetBlockByNumber(eqTo(blockParameter), eqTo(withTransactions))
      doReturn(FutureConverters.toJava(Future.successful(block)).toCompletableFuture).when(request).sendAsync()

      recoverToSucceededIf[ETHService.Error.CouldNotGetBlock](service.getBlock(blockNumber, withTransactions))
    }
  }

  private def getService(ethClient: Web3j): ETHService = {
    new ETHServiceRPCImpl(ethClient)(Helpers.Executors.blockingEC)
  }
}
