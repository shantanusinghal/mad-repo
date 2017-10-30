import re
import sys
from datetime import time, timedelta


class Block(object):
    def __init__(self, lines):
        self.index = int(lines[0])
        t = lines[1].split(' --> ')
        t_f = [int(x) for x in re.split(r'[:,]', t[0])]
        t_t = [int(x) for x in re.split(r'[:,]', t[1])]
        self.time_from = timedelta(hours=t_f[0], minutes=t_f[1], seconds=t_f[2], milliseconds=t_f[3])
        self.time_to = timedelta(hours=t_t[0], minutes=t_t[1], seconds=t_t[2], milliseconds=t_t[3])
        self.text = lines[2:]

    def offset(self, delta):
        self.time_from += delta
        self.time_to += delta
        return self

    def __str__(self):
        return '{:d}\n{:02d}:{:02d}:{:02d},{:03d} --> {:02d}:{:02d}:{:02d},{:03d}\n{}' \
            .format(self.index, self.time_from.seconds // 3600, (self.time_from.seconds // 60) % 60,
                    self.time_from.seconds % 60, self.time_from.microseconds // 1000, self.time_to.seconds // 3600,
                    (self.time_to.seconds // 60) % 60, self.time_to.seconds % 60, self.time_to.microseconds // 1000,
                    '\n'.join(self.text))


def main():
    # print command line arguments
    filename = sys.argv[1]
    index_form = int(sys.argv[2])
    index_to = int(sys.argv[3])
    offset_seconds = int(sys.argv[4])
    export_corrected_subs(filename, index_form, index_to, offset_seconds)


def export_corrected_subs(filename, index_form, index_to, offset_seconds):
    with open(filename) as f:
        lines = f.read().splitlines()
    # hack to remove BOM
    lines[0] = '1'

    block_map = read_blocks(lines)

    delta = timedelta(milliseconds=offset_seconds * 1000)

    for i in range(index_form, index_to + 1):
        block_map[i] = block_map[i].offset(delta)

    with open('corrected.srt', 'w') as f:
        f.write('\n\n'.join([x.__str__() for x in block_map.values()]))


def read_blocks(lines):
    length = len(lines)
    blocks = []
    i = 0
    while i < length:
        j = i + 1
        while j < length and lines[j] != '':
            j += 1
        blocks.append(Block(lines[i:j]))
        i = j
        while i < length and lines[i] == '':
            i += 1
    return {b.index: b for b in blocks}


if __name__ == "__main__":
    main()
