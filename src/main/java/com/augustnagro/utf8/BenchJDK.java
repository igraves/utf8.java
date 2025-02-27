package com.augustnagro.utf8;

import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 5)
@Measurement(time = 1, iterations = 1)
@Fork(
  value = 1, warmups = 1,
  jvmArgsPrepend = {
    "--enable-preview",
    "--add-modules=jdk.incubator.vector",
  }
)
public class BenchJDK {

  private static final LookupTables LUTS_128 = new LookupTables128();
  private static final LookupTables LUTS_256 = new LookupTables256();
  private static final LookupTables LUTS_512 = new LookupTables512();

  @Param({"/twitter.json"}) // the following could be added to the list: {"/utf8-demo.txt", "/utf8-demo-invalid.txt", "/20k.txt"}
  String testFile;

  byte[] bytes;
  MemorySegment ms;
  int len;

  @Setup
  public void setup() throws IOException {
    bytes = getClass().getResourceAsStream(testFile).readAllBytes();
    len = bytes.length;
    ByteBuffer buf = ByteBuffer.allocateDirect(bytes.length + 128).alignedSlice(64);
    buf.put(bytes);
    ByteBuffer sliced = buf.slice(0, bytes.length);
    ms = MemorySegment.ofBuffer(sliced);
  }

  @Benchmark
  public boolean jdk() {
    try {
      new String(bytes, StandardCharsets.UTF_8);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Benchmark
  public boolean scalar() {
    return Utf8.scalarValidUtf8(0, ms, len);
  }

  @Benchmark
  public boolean vector_512() {
    return Utf8.validate(ms, len, LUTS_512);
  }

  @Benchmark
  public boolean vector_256() {
    return Utf8.validate(ms, len, LUTS_256);
  }

  @Benchmark
  public boolean vector_128() {
    return Utf8.validate(ms, len, LUTS_128);
  }

}
