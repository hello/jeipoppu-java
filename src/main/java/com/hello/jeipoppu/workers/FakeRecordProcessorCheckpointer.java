package com.hello.jeipoppu.workers;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.model.Record;

public class FakeRecordProcessorCheckpointer implements IRecordProcessorCheckpointer {


  public FakeRecordProcessorCheckpointer() {

  }

  public void checkpoint() {

  }

  public void checkpoint(Record record) {

  }

  public void checkpoint(String sequenceNumber) {

  }

  public void checkpoint(String sequenceNumber, long subSequenceNumber) {

  }

}
