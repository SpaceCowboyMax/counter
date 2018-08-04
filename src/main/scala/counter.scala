import chisel3._

class SimpleCounter(width: Int = 32) extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val out = Output(UInt(width.W))
  })

  val counter = RegInit(0.U(width.W))

  io.out <> counter

  when(io.enable) {
    counter := counter + 1.U
  }
}

class MultiChannelCounter(width: Seq[Int] = Seq(32, 16, 8, 4)) extends Module {
  val io = IO(new Bundle {
    val enable = Input(Vec(width.length, Bool()))
    val out = Output(UInt(width.sum.W))

    def getOut(i: Int): UInt = {
      val right = width.dropRight(width.length - i).sum
      this.out(right + width(i) - 1, right)
    }
  })

  val counters: Seq[SimpleCounter] = width.map(x =>
    Module(new SimpleCounter(x))
  )

  io.out <> util.Cat(counters.map(_.io.out))

  width.indices.foreach { i =>
    counters(i).io.enable <> io.enable(i)
  }
}

class WishboneMultiChannelCounter extends Module {
  val BASE = 0x11A00000
  val OUT  = 0x00000100
  val S_EN = 0x00000200
  val H_EN = 0x00000300

  val wbAddrWidth = 32
  val wbDataWidth = 32
  val wbGotTag = false
  val width = Seq(32, 16, 8, 4)

  val io = IO(new wishboneSlave(wbAddrWidth, wbDataWidth, wbGotTag) {
    val hardwareEnable: Vec[Bool] = Input(Vec(width.length, Bool()))
  })

  val counter = Module(new MultiChannelCounter(width))

  val softwareEnable = RegInit(0.U(width.length.W))

  width.indices.foreach(i => counter.io.enable(i) := io.hardwareEnable(i) && softwareEnable(i))

  val readMemMap = Map(
    BASE + OUT  -> width.indices.map(counter.io.getOut),
    BASE + S_EN -> softwareEnable,
    BASE + H_EN -> io.hardwareEnable.asUInt
  )

  val writeMemMap = Map(
    BASE + S_EN -> softwareEnable
  )
}

class wishbone_multicahnnel_counter extends WishboneMultiChannelCounter with wishboneSlaveDriver

object countersDriver extends App {
  Driver.execute(Array("-td", "./src/generated"), () =>
    new wishbone_multicahnnel_counter
  )
}


