package io.stakenet.eth.explorer.models

case class SynchronizationProgress(total: BigInt, synced: BigInt) {
  def missing: BigInt = BigInt(0).max(total - synced)
}
