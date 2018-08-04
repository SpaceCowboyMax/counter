import chisel3.{Bundle, Driver, Module, Vec}

class WishboneCrossbarIo(n: Int, addrWidth: Int, dataWidth: Int) extends Bundle {
  val slaves = Vec(n, new wishboneSlave(addrWidth, dataWidth, false))
  val master = new wishboneMaster(addrWidth, dataWidth, false)
}

class WishboneCrossBar extends Module {
  val io = IO(new WishboneCrossbarIo(n = 1, 32, 32))
  io.master <> io.slaves(0)

  // ...
}

object WishboneCrossBar extends App {
  Driver.execute(Array("-td", "./src/generated"), () => new WishboneCrossBar)
}
