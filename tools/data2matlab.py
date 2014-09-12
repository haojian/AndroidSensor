#!/usr/bin/env python
#Author Phinfinity <rndanish@gmail.com>

import sys
import struct
import math
import datetime

RECORD_SIZE=20 # timestamp-8 , 3xfloats (3x4 = 12)
AVG_RANGE=20

def error_exit(s):
    print s
    sys.exit(2)

if len(sys.argv) != 3:
    error_exit("Usage : %s input_file output_name"%sys.argv[0])

import scipy.io

file_name = sys.argv[1]
f = open(file_name, 'rb')
f.seek(0,2)
sz = f.tell()
f.seek(0,0)
if (sz%2 != 0):
    error_exit("Corrupted file, invalid size")
n = sz/20
a = []
maxg = 0.0
maxAvg_g = 0.0
lAvg_g = 0.0
def calcg(p):
    return (p[1]*p[1]+p[2]*p[2]+p[3]*p[3])**0.5

for i in xrange(n):
    s = f.read(20)
    (t,x,y,z) = struct.unpack(">Qfff",s)
    a.append((t,x,y,z))
    maxg = max(maxg,calcg(a[-1]))
    if(i-AVG_RANGE>=0):
        lAvg_g-=calcg(a[i-AVG_RANGE])
    lAvg_g+=calcg(a[-1])
    maxAvg_g=max(maxAvg_g,lAvg_g/AVG_RANGE)
duration = a[-1][0] - a[0][0]
sys.stderr.write("Duration : %s\n"%(str(datetime.timedelta(seconds=duration/1000))))
sys.stderr.write("%d Data points\n"%len(a))
sys.stderr.write("Maximum Acceleration %f\n"%maxg)
sys.stderr.write("Maximum Acceleration (%d point Avg) %f\n"%(AVG_RANGE,maxAvg_g))
avg_g = reduce(lambda x,y: x+y, map(calcg,a))/len(a)
sys.stderr.write("Average Acceleration %f\n"%avg_g)
scipy.io.savemat(sys.argv[2],mdict={sys.argv[2]:a},oned_as='column')
