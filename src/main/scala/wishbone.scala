import chisel3._
import chisel3.util.MuxCase

class wishboneMasterSignals(
    addrWidth: Int = 32,
    dataWidth: Int = 32,
    gotTag: Boolean = false)
  extends Bundle {

  val adr = Output(UInt(addrWidth.W))
  val dat_master = Output(UInt(dataWidth.W))
  val dat_slave = Input(UInt(dataWidth.W))

  val stb = Output(Bool())
  val we = Output(Bool())
  val cyc = Output(Bool())

  val sel = Output(UInt((dataWidth / 8).W))
  val ack_master = Output(Bool())
  val ack_slave = Input(Bool())

  val tag_master: Option[UInt] = if(gotTag) Some(Output(Bool())) else None
  val tag_slave: Option[UInt] = if(gotTag) Some(Input(Bool())) else None

  def wbTransaction: Bool = cyc && stb
  def wbWrite: Bool = wbTransaction && we
  def wbRead: Bool = wbTransaction && !we

  override def cloneType: wishboneMasterSignals.this.type =
    new wishboneMasterSignals(addrWidth, dataWidth, gotTag).asInstanceOf[this.type]
}

class wishboneMaster(
    addrWidth: Int = 32,
    dataWidth: Int = 32,
    gotTag: Boolean = false)
  extends Bundle {

  val wb = new wishboneMasterSignals(addrWidth , dataWidth, gotTag)

  override def cloneType: wishboneMaster.this.type =
    new wishboneMaster(addrWidth, dataWidth, gotTag).asInstanceOf[this.type]
}

class wishboneSlave(
    addrWidth: Int = 32,
    dataWidth: Int = 32,
    gotTag: Boolean = false)
  extends Bundle {

  val wb = Flipped(new wishboneMasterSignals(addrWidth , dataWidth, gotTag))

  override def cloneType: wishboneSlave.this.type =
    new wishboneSlave(addrWidth, dataWidth, gotTag).asInstanceOf[this.type]
}

trait wishboneSlaveDriver {
  val io : wishboneSlave

  val readMemMap: Map[Int, Any]
  val writeMemMap: Map[Int, Any]

  val parsedReadMap: Seq[(Bool, UInt)] = parseMemMap(readMemMap)
  val parsedWriteMap: Seq[(Bool, UInt)] = parseMemMap(writeMemMap)

  val wb_ack = RegInit(false.B)
  val wb_dat = RegInit(0.U(io.wb.dat_slave.getWidth.W))

  when(io.wb.wbTransaction) {
    wb_ack := true.B
  }.otherwise {
    wb_ack := false.B
  }

  when(io.wb.wbRead) {
    wb_dat := MuxCase(default = 0.U, parsedReadMap)
  }

  when(io.wb.wbWrite) {
    parsedWriteMap.foreach { case(addrMatched, data) =>
      data := Mux(addrMatched, io.wb.dat_master, data)
    }
  }

  wb_dat <> io.wb.dat_slave
  wb_ack <> io.wb.ack_slave

  def parseMemMap(memMap: Map[Int, Any]): Seq[(Bool, UInt)] = memMap.flatMap { case(addr, data) =>
    data match {
      case a: UInt => Seq((io.wb.adr === addr.U) -> a)
      case b: Seq[UInt] => b.map(x => (io.wb.adr === (addr + b.indexOf(x) * io.wb.dat_slave.getWidth / 8).U) -> x)
      case _ => throw new Exception("WRONG MEM MAP!!!")
    }
  }.toSeq
}