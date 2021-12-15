package io.stakenet.eth.explorer.models

import io.stakenet.eth.explorer.Helpers
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TransactionSpec extends AnyWordSpec with Matchers {
  "tokenTransferRecipient" should {
    "get the correct address" in {
      val transferInput =
        "0xa9059cbb0000000000000000000000000fb342955b20fa658e0cb7ff50902b7ea097b7fd0000000000000000000000000000000000000000000000000000000004c4b400"
      val transaction = Helpers.randomTransaction().copy(input = transferInput)

      transaction.tokenTransferRecipient mustBe Some("0x0fb342955b20fa658e0cb7ff50902b7ea097b7fd")
    }

    "return None when transaction is not a token transfer" in {
      val emptyInput = "0x"
      val transaction = Helpers.randomTransaction().copy(input = emptyInput)

      transaction.tokenTransferRecipient mustBe None
    }
  }
}
