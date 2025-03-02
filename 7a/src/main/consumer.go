package main

import (
	"fmt"
	"log"

	"github.com/streadway/amqp"
)

func main() {
	// Connect to RabbitMQ
	conn, err := amqp.Dial("amqp://guest:guest@localhost:5672/")
	if err != nil {
		log.Fatalf("Failed to connect to RabbitMQ: %v", err)
	}
	defer conn.Close()

	// Open a channel
	ch, err := conn.Channel()
	if err != nil {
		log.Fatalf("Failed to open a channel: %v", err)
	}
	defer ch.Close()

	// Declare the queue (should match the producer)
	queue, err := ch.QueueDeclare(
		"album_queue", false, false, false, false, nil,
	)
	if err != nil {
		log.Fatalf("Failed to declare queue: %v", err)
	}

	// Start consuming messages
	msgs, err := ch.Consume(
		queue.Name, // Queue name
		"",         // Consumer name (empty means auto-generated)
		false,      // Auto-Ack (set to false so we manually ack)
		false,      // Exclusive
		false,      // No-local
		false,      // No-wait
		nil,        // Args
	)
	if err != nil {
		log.Fatalf("Failed to consume messages: %v", err)
	}

	// Process messages in a separate goroutine
	go func() {
		for msg := range msgs {
			fmt.Printf("Received message: %s\n", msg.Body)

			// Here you can process the message, e.g., store in DB

			// Manually acknowledge the message
			msg.Ack(false)
		}
	}()

	log.Println("Consumer is running... Waiting for messages.")
	select {} // Keep running forever
}
