package com.hello.jeipoppu.classifiers;

import com.hello.jeipoppu.models.Classification;
import com.hello.suripu.api.audio.MatrixProtos.MatrixClientMessage;

public interface Classifier {

  public Classifier create(MatrixClientMessage message);
  public Classification run();
}
