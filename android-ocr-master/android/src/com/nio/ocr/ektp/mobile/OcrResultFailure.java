package com.nio.ocr.ektp.mobile;

public final class OcrResultFailure {
  private final long timestamp;
  
  OcrResultFailure() {
    this.timestamp = System.currentTimeMillis();
  }
  

  public long getTimestamp() {
    return timestamp;
  }
  
}
