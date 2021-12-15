# Install go-ethereum
[go-ethereum](geth.ethereum.org) is the recommended Ethereum Node, these instructions should be enough to install it in ubuntu 20.04.Adapt the instructions to match your needs.

Install dependencies:
- `sudo apt-get install software-properties-common`

Enable the custom repository:
- `sudo add-apt-repository -y ppa:ethereum/ethereum`

Install:
- `sudo apt-get update && sudo apt-get install ethereum`

Run the node in mainnet with:
- `/usr/bin/geth --syncmode fast --http --datadir /mnt/volume_eth_indexer/db/`

Which uses the following options:
- `--syncmode fast` enables fast syncing.
- `--http` enables the rest api.
- `--datadir /mnt/volume_eth_indexer/db/` uses a custom directory to store the data.

Adding `--ropsten` would use the ropsten network instead.

Be sure to check the official docs to see all the options: https://geth.ethereum.org/docs/interface/command-line-options


## systemd service
When running geth in a server, it is a good idea to use systemd to manage the service, which can be done by creating the file `/etc/systemd/geth.service`:

```
[Unit]
Description=geth

[Service]
Type=simple
StandardOutput=tty
StandardError=tty
LimitNOFILE=65535
User=ubuntu
ExecStart=/usr/bin/geth --syncmode fast --http --ropsten
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Once the file is created, run `sudo systemctl daemon-reload` to let systemd pick the changes.

Then:
- Start the service: `sudo service geth start`
- Stop the service: `sudo service geth stop`
- Check the status: `sudo service geth status`

Check the syncing status by running `geth attach http://127.0.0.1:8545` and then `eth.syncing`, it can take several minutes for the node to start syncing.

