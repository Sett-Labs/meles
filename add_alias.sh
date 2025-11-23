#!/bin/bash
echo "alias meles_restart='sudo systemctl restart meles'" >> ~/.bashrc
echo "alias meles_start='sudo systemctl start meles'" >> ~/.bashrc
echo "alias meles_stop='sudo systemctl stop meles'" >> ~/.bashrc
echo "alias meles_log='sudo journalctl -u meles.service'" >> ~/.bashrc
echo "alias meles_track='sudo journalctl -u meles.service -f'" >> ~/.bashrc
echo "alias meles='telnet localhost 2323'" >> ~/.bashrc
echo "alias das='telnet localhost 2323'" >> ~/.bashrc