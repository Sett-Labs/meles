#!/bin/bash
SCRIPT="$(readlink --canonicalize-existing "$0")"
SCRIPT_PATH="$(dirname "$SCRIPT")"

echo "[Unit]" >> meles.service
echo "Description=meles Data Acquisition System Service" >> meles.service
echo "[Service]" >> meles.service
echo "Type=simple" >> meles.service
echo "ExecStart=/bin/sh -c 'java -jar $SCRIPT_PATH/meles-*.jar'" >> meles.service
echo "Restart=on-failure" >> meles.service
echo "RestartSec=3s" >> meles.service
echo "[Install]" >> meles.service
echo "WantedBy=default.target" >> meles.service
sudo mv meles.service /etc/systemd/user

# Create the alias's
echo "alias meles_restart='systemctl --user restart meles.service'" >> ~/.bashrc
echo "alias meles_status='systemctl --user status meles.service'" >> ~/.bashrc
echo "alias meles_start='systemctl --user start meles.service'" >> ~/.bashrc
echo "alias meles_stop='systemctl --user stop meles.service'" >> ~/.bashrc
echo "alias meles_log='sudo journalctl --user -u meles.service'" >> ~/.bashrc
echo "alias meles_track='sudo journalctl --user -u meles.service -f'" >> ~/.bashrc
echo "alias meles='telnet localhost 2323'" >> ~/.bashrc
# Apply those changes
source ~/.bashrc

# Add user to dialout because needed for serialports
sudo adduser $USER dialout

systemctl --user daemon-reload
systemctl --user enable meles.service
systemctl --user start meles.service