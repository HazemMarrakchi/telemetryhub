import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'fileSize', standalone: true })
export class FileSizePipe implements PipeTransform {
  transform(bytes: number | null | undefined): string {
    if (!bytes || bytes <= 0) {
      return '—';
    }
    const units = ['o', 'Ko', 'Mo', 'Go'];
    let value = bytes;
    let index = 0;
    while (value >= 1024 && index < units.length - 1) {
      value /= 1024;
      index++;
    }
    return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
  }
}