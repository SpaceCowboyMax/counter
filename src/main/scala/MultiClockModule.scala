import chisel3._
import chisel3.experimental._


class DoubleClockModule extends Module {
  val io = IO(new Bundle {
    val clockB = Input(Clock())

    val in = Input(Bool())
    val out = Output(Bool())
    val outB = Output(Bool())
  })

  val regClock = RegNext(io.in, false.B)

  regClock <> io.out

  val regClockB = withClock(io.clockB) {
    RegNext(io.in, false.B)
  }

  regClockB <> io.outB
}


class MultiClockModule extends RawModule {
  val io = IO(new Bundle {
    val clockA = Input(Clock())
    val clockB = Input(Clock())
    val reasetA = Input(Bool())
    val reasetB = Input(Bool())

    val in = Input(Bool())
    val outA = Output(Bool())
    val outB = Output(Bool())
  })

  val regClockA = withClockAndReset(io.clockA, io.reasetA) {
    RegNext(io.in, false.B)
  }

  regClockA <> io.outA

  val regClockB = withClockAndReset (io.clockB, io.reasetB) {
     RegNext(io.in, false.B)
  }

  regClockB <> io.outB
}

object MultiClockModule extends App {
  Driver.execute(Array("-td", "./src/generated"), () => new MultiClockModule)
}

object DoubleClockModule extends App {
  Driver.execute(Array("-td", "./src/generated"), () => new DoubleClockModule)
}