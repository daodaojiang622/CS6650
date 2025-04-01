**Server for HW9**

To start the homework:
1. Edit the database connection info in the Server.java and ReviewConsumer.java
2. Run consumer first
3. Run Server
4. Go to the Client folder and read the README there

To open the RabbitMQ management console:
1. If you are running on localhost, go to localhost:15672
2. If you are running on a VM, go to the VM's ip address:15672
3. Login with guest/guest

If Server is running on a VM, use docker to run RabbitMQ locally:
1. Open an SSH channel between EC2 and your local machine
    `ssh -i your-key.pem -N -R 5672:localhost:5672 ec2-user@<your-ec2-public-ip>`
