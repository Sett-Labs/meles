#!/bin/bash

# Stop and disable the service
sudo systemctl stop meles.service
sudo systemctl disable meles.service

# Remove the service definition
sudo rm -f /lib/systemd/system/meles.service

# Reload the systemd manager configuration
sudo systemctl daemon-reload

# Reset the failed state of the service
sudo systemctl reset-failed

