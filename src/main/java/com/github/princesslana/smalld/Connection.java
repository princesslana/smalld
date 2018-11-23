package com.github.princesslana.smalld;

public interface Connection extends AutoCloseable {

  void await();

  void close();
}
