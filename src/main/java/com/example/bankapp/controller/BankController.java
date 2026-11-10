package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;
    private final BankService bankService;
    private final TransactionAuditService auditService;

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("customers", keyword == null || keyword.isBlank()
                ? customerRepository.findAllOrdered()
                : customerRepository.searchByName(keyword));
        model.addAttribute("allCustomers", customerRepository.findAll());
        model.addAttribute("transactions", transactionRepository.findAllLatest());
        model.addAttribute("totalBalance", customerRepository.getTotalBalance());
        model.addAttribute("totalCustomers", customerRepository.count());
        model.addAttribute("successCount", transactionRepository.countByStatus(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", transactionRepository.countByStatus(TransactionStatus.FAILED));
        model.addAttribute("transferRequest", new TransferRequest());
        model.addAttribute("customer", new Customer());
        model.addAttribute("keyword", keyword);
        return "bank";
    }

    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                           BindingResult bindingResult, RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            redirect.addFlashAttribute("error", "Please enter valid transfer details.");
            return "redirect:/";
        }
        try {
            bankService.transfer(request.getFromAccount(), request.getToAccount(),
                    request.getAmount(), request.isSimulateFailure());
            redirect.addFlashAttribute("success", "Transfer completed successfully!");
        } catch (Exception ex) {
            if (isAuditReady(request.getFromAccount(), request.getToAccount(), request.getAmount())) {
                try {
                    auditService.recordFailure(request.getFromAccount(), request.getToAccount(),
                            request.getAmount(), ex.getMessage());
                } catch (Exception auditEx) {
                    redirect.addFlashAttribute("error", "Transfer failed; failed audit could not be recorded: " + auditEx.getMessage());
                    return "redirect:/";
                }
            }
            redirect.addFlashAttribute("error", "Transfer failed: " + ex.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/customers")
    public String addCustomer(@Valid @ModelAttribute("customer") Customer customer,
                              BindingResult bindingResult, RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            redirect.addFlashAttribute("error", "Please enter valid customer details.");
            return "redirect:/";
        }
        if (customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
            redirect.addFlashAttribute("error", "Account number already exists.");
            return "redirect:/";
        }
        if (customerRepository.countEmails(customer.getEmail()) > 0) {
            redirect.addFlashAttribute("error", "Email address already exists.");
            return "redirect:/";
        }
        customerRepository.save(customer);
        redirect.addFlashAttribute("success", "Customer added successfully!");
        return "redirect:/";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(@PathVariable String accountNumber, Model model,
                                 RedirectAttributes redirect) {
        Customer customer = customerRepository.getByAccountNumber(accountNumber).orElse(null);
        if (customer == null) {
            redirect.addFlashAttribute("error", "Account not found.");
            return "redirect:/";
        }
        model.addAttribute("customer", customer);
        model.addAttribute("transactions", transactionRepository.findHistory(accountNumber));
        model.addAttribute("totalSent", transactionRepository.calculateTotalSent(
                accountNumber, TransactionStatus.SUCCESS));
        return "history";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min, Model model) {
        BigDecimal safeMin = min.max(BigDecimal.ZERO);
        model.addAttribute("min", safeMin);
        model.addAttribute("transfers", transactionRepository.findLargeTransfers(
                TransactionStatus.SUCCESS, safeMin));
        return "large-transfers";
    }

    @GetMapping("/deposit")
    public String depositPage(Model model) {
        model.addAttribute("customers", customerRepository.findAllOrdered());
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam String accountNumber, @RequestParam BigDecimal amount,
                          RedirectAttributes redirect) {
        try {
            bankService.deposit(accountNumber, amount);
            redirect.addFlashAttribute("success", "Deposit completed successfully.");
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", "Deposit failed: " + ex.getMessage());
        }
        return "redirect:/deposit";
    }

    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        model.addAttribute("customers", customerRepository.findAllOrdered());
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam String accountNumber, @RequestParam BigDecimal amount,
                           RedirectAttributes redirect) {
        try {
            bankService.withdraw(accountNumber, amount);
            redirect.addFlashAttribute("success", "Withdrawal completed successfully.");
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", "Withdrawal failed: " + ex.getMessage());
        }
        return "redirect:/withdraw";
    }

    private boolean isAuditReady(String from, String to, BigDecimal amount) {
        return from != null && !from.isBlank() && to != null && !to.isBlank()
                && amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
