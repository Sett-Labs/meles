#!/bin/bash
echo "alias meles_restart='sudo systemctl restart meles'" >> ~/.bash_aliases
echo "alias meles_start='sudo systemctl start meles'" >> ~/.bash_aliases
echo "alias meles_stop='sudo systemctl stop meles'" >> ~/.bash_aliases
echo "alias meles_log='sudo journalctl -u meles.service'" >> ~/.bash_aliases
echo "alias meles_track='sudo journalctl -u meles.service -f'" >> ~/.bash_aliases
echo "alias meles='telnet localhost 2323'" >> ~/.bash_aliases