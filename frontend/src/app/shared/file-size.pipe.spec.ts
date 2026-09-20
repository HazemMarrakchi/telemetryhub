import { Pipe, PipeTransform } from '@angular/core';
import { FileSizePipe } from './file-size.pipe';

describe('FileSizePipe', () => {
  let pipe: FileSizePipe;

  beforeEach(() => (pipe = new FileSizePipe()));

  it('returns dash for null', () => {
    expect(pipe.transform(null as any)).toBe('—');
  });

  it('returns dash for undefined', () => {
    expect(pipe.transform(undefined as any)).toBe('—');
  });

  it('returns dash for zero', () => {
    expect(pipe.transform(0)).toBe('—');
  });

  it('returns dash for negative', () => {
    expect(pipe.transform(-1)).toBe('—');
  });

  it('formats bytes as whole number', () => {
    expect(pipe.transform(512)).toBe('512 o');
  });

  it('formats kilobytes with one decimal', () => {
    expect(pipe.transform(1024)).toBe('1.0 Ko');
    expect(pipe.transform(1536)).toBe('1.5 Ko');
    expect(pipe.transform(10240)).toBe('10.0 Ko');
  });

  it('formats megabytes with one decimal', () => {
    expect(pipe.transform(1048576)).toBe('1.0 Mo');
    expect(pipe.transform(5242880)).toBe('5.0 Mo');
  });

  it('formats gigabytes with one decimal', () => {
    expect(pipe.transform(1073741824)).toBe('1.0 Go');
  });
});