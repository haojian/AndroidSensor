#!/usr/bin/env python
#Author Phinfinity <rndanish@gmail.com>

import sys
RECORD_SIZE=20 # timestamp-8 , 3xfloats (3x4 = 12)
AVG_RANGE=20

def error_exit(s):
    print s
    sys.exit(2)
if len(sys.argv) != 2:
    error_exit("Usage : %s input_file"%sys.argv[0])
import struct
import matplotlib
import pylab
import numpy as np
import math
import datetime


if(len(sys.argv) == 3):
    AVG_RANGE = int(sys.argv[2])
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
X = map(lambda x: pylab.date2num(datetime.datetime.fromtimestamp(x[0]/1000.0)),a)
G = map(calcg, a)
G_x = map(lambda x : x[1] , a)
G_y = map(lambda x : x[2] , a)
G_z = map(lambda x : x[3] , a)
pylab.plot_date(X,G,"bo",xdate=True,ydate=False)
pylab.plot_date(X,G,"r-",xdate=True,ydate=False)

pylab.plot_date(X,G_x,"g-",xdate=True,ydate=False)
pylab.plot_date(X,G_y,"c-",xdate=True,ydate=False)
pylab.plot_date(X,G_z,"k-",xdate=True,ydate=False)
pylab.show()
